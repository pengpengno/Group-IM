package com.github.im.server.service;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.exception.BusinessException;
import com.github.im.server.mapstruct.DepartmentMapper;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.model.UserDepartment;
import com.github.im.server.repository.DepartmentRepository;
import com.github.im.server.repository.UserDepartmentRepository;
import com.github.im.server.util.SchemaSwitcher;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final DepartmentRepository departmentRepository;
    private final DepartmentMapper departmentMapper;
    private final CompanyUserService companyUserService;
    private final CompanyService companyService;
    private final EntityManager entityManager;
    private final UserService userService;
    private final UserDepartmentRepository userDepartmentRepository;

    /**
     * 创建部门
     * @param departmentDTO 部门信息
     * @return 创建后的部门信息
     */
    @Transactional
    public DepartmentDTO createDepartment(DepartmentDTO departmentDTO) {
        String schemaName = companyService.getSchemaNameByCompanyId(departmentDTO.getCompanyId());
        
        return SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            Department department = new Department();
            department.setName(departmentDTO.getName());
            department.setDescription(departmentDTO.getDescription());
            department.setCompanyId(departmentDTO.getCompanyId());
            department.setParentId(departmentDTO.getParentId());
            department.setOrderNum(departmentDTO.getOrderNum());
            department.setStatus(departmentDTO.getStatus() != null ? departmentDTO.getStatus() : true);
            
            Department savedDepartment = departmentRepository.save(department);
            return departmentMapper.departmentToDepartmentDTO(savedDepartment);
        });
    }

    /**
     * 更新部门信息
     * @param departmentId 部门ID
     * @param departmentDTO 部门信息
     * @return 更新后的部门信息
     */
    @Transactional
    public DepartmentDTO updateDepartment(Long departmentId, DepartmentDTO departmentDTO) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        
        String schemaName = companyService.getSchemaNameByCompanyId(department.getCompanyId());
        
        return SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            department.setName(departmentDTO.getName());
            department.setDescription(departmentDTO.getDescription());
            if (departmentDTO.getOrderNum() != null) {
                department.setOrderNum(departmentDTO.getOrderNum());
            }
            if (departmentDTO.getStatus() != null) {
                department.setStatus(departmentDTO.getStatus());
            }
            
            Department savedDepartment = departmentRepository.save(department);
            return departmentMapper.departmentToDepartmentDTO(savedDepartment);
        });
    }

    /**
     * 删除部门
     * @param departmentId 部门ID
     */
    @Transactional
    public void deleteDepartment(Long departmentId) {
        // 先找到 部门 所属的 公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("部门不存在!"));
        

        var count = userDepartmentRepository.deleteByDepartmentId(departmentId);

        departmentRepository.deleteById(departmentId);


    }

    /**
     * 移动部门到新的父部门下
     * @param departmentId 部门ID
     * @param newParentId 新的父部门ID，如果为null则移动到根节点
     * @return 移动后的部门信息
     */
    @Transactional
    public DepartmentDTO moveDepartment(Long departmentId, Long newParentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        
        department.setParentId(newParentId);
        Department savedDepartment = departmentRepository.save(department);
        return departmentMapper.departmentToDepartmentDTO(savedDepartment);

    }

    /**
     * 将用户分配到部门（移动用户到新部门）
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    @Transactional
    public void assignUserToDepartment(Long userId, Long departmentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        
        // 验证用户是否属于当前公司
        if (!companyUserService.isUserInCompany(userId, department.getCompanyId())) {
            throw new RuntimeException("用户不属于当前公司");
        }

        // 先删除用户在当前部门的关联关系
        userDepartmentRepository.deleteByUserId(userId);
        userDepartmentRepository.flush(); // 删除了避免 HIB 中对于sql的执行顺序  ，先删除

        userDepartmentRepository.save(new UserDepartment(userId,departmentId));

    }

    /**
     * 批量将用户分配到部门（移动用户到新部门）
     * @param userIds 用户ID列表
     * @param departmentId 部门ID
     */
    @Transactional
    public void batchAssignUsersToDepartment(List<Long> userIds, Long departmentId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("部门不存在"));
        

        // 验证所有用户是否存在于当前公司中
        List<Long> validUserIds = companyUserService.getValidUserIdsInCompany(userIds, department.getCompanyId());

        if (validUserIds.size() != userIds.size()) {
            // 检查哪些用户无效
            List<Long> invalidUserIds = new ArrayList<>(userIds);
            invalidUserIds.removeAll(validUserIds);
            throw new BusinessException("以下用户不属于当前公司: " + invalidUserIds);
        }
        userDepartmentRepository.deleteByUserIdIn(userIds);
        userDepartmentRepository.flush(); // 删除了避免 HIB 中对于sql的执行顺序  ，先删除

        List<UserDepartment> userDepartments = userIds.stream().map(uId -> {
            return new UserDepartment(uId, departmentId);
        }).toList();
        userDepartmentRepository.saveAll(userDepartments);

    }

    /**
     * 将用户从部门移除
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    @Transactional
    public void removeUserFromDepartment(Long userId, Long departmentId) {
        // 先找到部门所属的公司
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        userDepartmentRepository.deleteByUserIdAndDepartmentId(userId, departmentId);
    }

    /**
     * 删除部门相关的用户关系
     * @param em EntityManager
     * @param departmentId 部门ID
     */
    private void deleteUserDepartmentByDepartmentId(EntityManager em, Long departmentId) {
        // 删除用户部门关联关系
        em.createQuery("DELETE FROM UserDepartment ud WHERE ud.departmentId = :departmentId")
                .setParameter("departmentId", departmentId)
                .executeUpdate();
    }

    /**
     * 调整组织架构
     * @param companyId 公司ID
     * @param departmentDTOS 新的组织架构信息
     */
    @Transactional
    public void adjustOrganizationStructure(Long companyId, List<DepartmentDTO> departmentDTOS) {
        String schemaName = companyService.getSchemaNameByCompanyId(companyId);
        
        SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            // 删除现有所有部门
            departmentRepository.deleteByCompanyId(companyId);
            
            // 重新创建部门结构
            createDepartmentsRecursive(null, departmentDTOS, companyId);
        });
    }
    
    /**
     * 递归创建部门结构
     * @param parentId 父部门ID
     * @param children 子部门列表
     * @param companyId 公司ID
     */
    private void createDepartmentsRecursive(Long parentId, List<DepartmentDTO> children, Long companyId) {
        if (children == null || children.isEmpty()) {
            return;
        }
        
        for (DepartmentDTO dto : children) {
            Department department = new Department();
            department.setName(dto.getName());
            department.setDescription(dto.getDescription());
            department.setCompanyId(companyId);
            department.setParentId(parentId);
            department.setOrderNum(dto.getOrderNum());
            department.setStatus(dto.getStatus() != null ? dto.getStatus() : true);
            
            Department savedDepartment = departmentRepository.save(department);
            
            // 递归处理子部门
            createDepartmentsRecursive(savedDepartment.getDepartmentId(), dto.getChildren(), companyId);
        }
    }

    /**
     * 获取组织架构树
     * @param companyId 公司ID
     * @return 组织架构树
     */
    @Cacheable(value = "organizationStructure", key = "#companyId")
    public List<Department> getOrganizationStructure(Long companyId) {
        // 获取公司下所有启用的部门

        List<Department> allDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);

        // 构建部门树结构
        Map<Long, Department> departmentMap = new HashMap<>();
        List<Department> rootDepartments = new ArrayList<>();
        
        // 初始化所有部门到Map中
        for (Department dept : allDepartments) {
            departmentMap.put(dept.getDepartmentId(), dept);
        }
        
        // 构建父子关系
        for (Department dept : allDepartments) {
            if (dept.getParentId() == null) {
                rootDepartments.add(dept);
            } else {
                Department parent = departmentMap.get(dept.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(dept);
                }
            }
        }
        
        return rootDepartments;
    }

    /**
     * 获取用户当前登录公司的信息
     * @param user
     * @return 返回部门Dto
     */
    public List<DepartmentDTO> getDepartmentDTOs(User user) {
        return departmentMapper.departmentsToDepartmentDTOs(getOrganizationStructure(user.getCurrentCompany().getCompanyId()));
    }

    /**
     * 获取指定公司的部门信息
     * @return 返回部门信息
     */
    public List<DepartmentDTO> getDepartmentDTOs(Long companyUserId) {
        // 查询数据
        String schemaName = companyService.getSchemaNameByCompanyId(companyUserId);
        return SchemaSwitcher.executeInSchema( schemaName, () ->
            departmentMapper.departmentsToDepartmentDTOs(getOrganizationStructure(companyUserId))
        );
    }
    
    /**
     * 获取用户的组织架构（仅包含该用户所在公司）
     * @param user 用户
     * @return 组织架构树
     */
    public List<Department> getUserOrganizationStructure(User user) {
        if (user.getCurrentCompany() == null) {
            return new ArrayList<>();
        }
        
        return getOrganizationStructure(user.getCurrentCompany().getCompanyId());
    }

    /**
     * 导入部门数据
     * 只需要操作 Schema 下的数据。
     * @param file Excel文件
     * @param companyId 公司ID
     */
    @Transactional
    public void importDepartments(MultipartFile file, Long companyId){
        String schemaName = companyService.getSchemaNameByCompanyId(companyId);
        SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
            try (InputStream inputStream = file.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0);

                // 获取该公司现有的所有部门，用于名称到ID的映射
                List<Department> existingDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);
                Map<String, Long> departmentNameToIdMap = existingDepartments.stream()
                        .collect(Collectors.toMap(Department::getName, Department::getDepartmentId));

                // 存储新创建的部门，以便后续引用
                Map<String, Long> newDepartmentNameToIdMap = new HashMap<>();

                // 从第二行开始读取数据（跳过标题行）
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    String departmentName = getCellValueAsString(row.getCell(0));
                    String parentDepartmentName = getCellValueAsString(row.getCell(1));
                    String description = getCellValueAsString(row.getCell(2));

                    if (departmentName == null || departmentName.trim().isEmpty()) {
                        throw new IllegalArgumentException("第" + (i + 1) + "行: 部门名称不能为空");
                    }

                    // 检查部门是否已存在
                    if (departmentNameToIdMap.containsKey(departmentName) ||
                        newDepartmentNameToIdMap.containsKey(departmentName)) {
                        throw new IllegalArgumentException("第" + (i + 1) + "行: 部门 '" + departmentName + "' 已存在");
                    }

                    Department department = new Department();
                    department.setName(departmentName);
                    department.setDescription(description);
                    department.setCompanyId(companyId);
                    department.setStatus(true);

                    // 设置父部门ID
                    if (parentDepartmentName != null && !parentDepartmentName.trim().isEmpty()) {
                        Long parentId = departmentNameToIdMap.get(parentDepartmentName);
                        if (parentId == null) {
                            parentId = newDepartmentNameToIdMap.get(parentDepartmentName);
                        }

                        if (parentId == null) {
                            throw new IllegalArgumentException("第" + (i + 1) + "行: 找不到父部门 '" + parentDepartmentName + "'");
                        }
                        department.setParentId(parentId);
                    }

                    // 保存部门并记录ID
                    Department savedDepartment = departmentRepository.save(department);
                    newDepartmentNameToIdMap.put(departmentName, savedDepartment.getDepartmentId());
                    log.info("成功导入部门: {}", departmentName);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }


    /**
     * 获取单元格的字符串值
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return null;
            default:
                return null;
        }
    }
    
    /**
     * 导入员工数据
     * @param file Excel文件
     * @param companyId 公司ID
     */
//    @Transactional
    public void importEmployees(MultipartFile file, Long companyId){
        String schemaName = companyService.getSchemaNameByCompanyId(companyId);
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);

            // 使用全新的连接在公司schema中获取现有的所有部门，用于名称到ID的映射
            List<Department> existingDepartments = SchemaSwitcher.executeWithFreshConnectionInSchema(
                entityManager, schemaName, em -> {
                    return em.createQuery(
                        "SELECT d FROM Department d WHERE d.companyId = :companyId AND d.status = true", 
                        Department.class)
                        .setParameter("companyId", companyId)
                        .getResultList();
                });

            Map<String, Long> departmentNameToIdMap = existingDepartments.stream()
                    .collect(Collectors.toMap(Department::getName, Department::getDepartmentId));

            // 在public schema中获取公司所有现有用户，用于用户名和邮箱的唯一性校验
            List<User> existingUsers = SchemaSwitcher.executeInPublicSchema(() -> 
                entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.primaryCompanyId = :companyId",
                    User.class)
                   .setParameter("companyId", companyId)
                   .getResultList()
            );

            // 从第二行开始读取数据（跳过标题行）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String username = getCellValueAsString(row.getCell(0));
                String email = getCellValueAsString(row.getCell(1));
                String phoneNumber = getCellValueAsString(row.getCell(2));
                String departmentName = getCellValueAsString(row.getCell(3));

                // 基本验证
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("第" + (i + 1) + "行: 用户名不能为空");
                }

                // 检查用户是否已存在
                boolean userExists = existingUsers.stream()
                        .anyMatch(u -> username.equals(u.getUsername()) ||
                                (email != null && email.equals(u.getEmail())));

                if (userExists) {
                    // 存在则 跳过 记录日志即可
                    log.info("用户已存在: {}", username);
                    continue;
                }

                // 在public schema 中保存用户
                User savedUser =  userService.createDefaultUser(username, email, phoneNumber, companyId);

                // 添加用户到公司（在company_user表中创建记录）
                companyUserService.addUserToCompany(savedUser.getUserId(), companyId);

                // 如果指定了部门，则在公司schema中建立用户部门关联
                if (departmentName != null && !departmentName.trim().isEmpty()) {
                    Long departmentId = departmentNameToIdMap.get(departmentName);
                    if (departmentId == null) {
                        throw new IllegalArgumentException("第" + (i + 1) + "行: 找不到部门 '" + departmentName + "'");
                    } else {
                        SchemaSwitcher.executeWithFreshConnectionInSchema(entityManager, schemaName, em -> {
                            UserDepartment userDepartment = new UserDepartment(savedUser.getUserId(), departmentId);
                            em.persist(userDepartment);
                        });
                    }
                }

                log.info("成功导入用户: {}", username);
            }
        } catch (Exception e) {
            log.error("导入员工数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("导入员工数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 生成部门导入Excel模板
     * @param outputStream 输出流
     * @throws IOException IO异常
     */
    public void generateDepartmentsImportTemplate(OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("部门导入模板");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("部门名称(name)*");
        headerRow.createCell(1).setCellValue("父部门名称(parentName)");
        headerRow.createCell(2).setCellValue("部门描述(description)");
        
        // 添加示例行
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("人力资源部");
        exampleRow.createCell(1).setCellValue(""); // 顶级部门
        exampleRow.createCell(2).setCellValue("负责人力资源管理");
        
        Row exampleRow2 = sheet.createRow(2);
        exampleRow2.createCell(0).setCellValue("招聘组");
        exampleRow2.createCell(1).setCellValue("人力资源部"); // 子部门
        exampleRow2.createCell(2).setCellValue("负责招聘工作");
        
        // 调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        
        // 冻结首行
        sheet.createFreezePane(0, 1);
        
        // 写入输出流
        workbook.write(outputStream);
        workbook.close();
    }
    
    /**
     * 生成员工导入Excel模板
     * @param outputStream 输出流
     * @throws IOException IO异常
     */
    public void generateEmployeesImportTemplate(OutputStream outputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("员工导入模板");
        
        // 创建标题行
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("用户名(username)*");
        headerRow.createCell(1).setCellValue("邮箱(email)");
        headerRow.createCell(2).setCellValue("手机号(phoneNumber)");
        headerRow.createCell(3).setCellValue("部门名称(departmentName)");
        
        // 添加示例行
        Row exampleRow = sheet.createRow(1);
        exampleRow.createCell(0).setCellValue("zhangsan");
        exampleRow.createCell(1).setCellValue("zhangsan@example.com");
        exampleRow.createCell(2).setCellValue("13800138000");
        exampleRow.createCell(3).setCellValue("人力资源部");
        
        Row exampleRow2 = sheet.createRow(2);
        exampleRow2.createCell(0).setCellValue("lisi");
        exampleRow2.createCell(1).setCellValue("lisi@example.com");
        exampleRow2.createCell(2).setCellValue("13800138001");
        exampleRow2.createCell(3).setCellValue("招聘组");
        
        // 调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        
        // 冻结首行
        sheet.createFreezePane(0, 1);
        
        // 写入输出流
        workbook.write(outputStream);
        workbook.close();
    }
}