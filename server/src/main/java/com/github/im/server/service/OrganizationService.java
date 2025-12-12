package com.github.im.server.service;

import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.mapstruct.OrganizationMapper;
import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.model.UserDepartment;
import com.github.im.server.repository.CompanyRepository;
import com.github.im.server.repository.DepartmentRepository;
import com.github.im.server.repository.UserDepartmentRepository;
import com.github.im.server.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    
    private final CompanyRepository companyRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final UserDepartmentRepository userDepartmentRepository;
    private final OrganizationMapper organizationMapper;
    
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
    public List<DepartmentDTO> getDepartmentDTOs(User user) {
        return organizationMapper.departmentsToDepartmentDTOs(getOrganizationStructure(user.getCurrentLoginCompanyId()));
    }
    /**
     * 获取用户的组织架构（仅包含该用户所在公司）
     * @param user 用户
     * @return 组织架构树
     */
    public List<Department> getUserOrganizationStructure(User user) {
        if (user.getCurrentLoginCompanyId() == null) {
            return new ArrayList<>();
        }
        
        return getOrganizationStructure(user.getCurrentLoginCompanyId());
    }
    
    /**
     * 导入部门数据
     * @param file Excel文件
     * @param companyId 公司ID
     */
    public void importDepartments(MultipartFile file, Long companyId) throws Exception {
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
                    continue; // 跳过空行
                }
                
                // 检查部门是否已存在
                if (departmentNameToIdMap.containsKey(departmentName) || 
                    newDepartmentNameToIdMap.containsKey(departmentName)) {
                    // 部门已存在，只记录日志而不抛出异常
                    log.warn("部门 '{}' 已存在，跳过导入", departmentName);
                    continue;
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
                        log.warn("找不到父部门: {}，将创建为顶级部门", parentDepartmentName);
                        // 如果找不到父部门，将其设置为顶级部门而不是抛出异常
                    }
                    department.setParentId(parentId);
                }
                
                // 保存部门并记录ID
                Department savedDepartment = departmentRepository.save(department);
                newDepartmentNameToIdMap.put(departmentName, savedDepartment.getDepartmentId());
                log.info("成功导入部门: {}", departmentName);
            }
        }
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
    public void importEmployees(MultipartFile file, Long companyId) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // 获取该公司现有的所有部门，用于名称到ID的映射
            List<Department> existingDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);
            Map<String, Long> departmentNameToIdMap = existingDepartments.stream()
                    .collect(Collectors.toMap(Department::getName, Department::getDepartmentId));
            
            // 获取公司所有现有用户，用于用户名和邮箱的唯一性校验
            List<User> existingUsers = userRepository.findByPrimaryCompanyId(companyId);
            
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
                    log.warn("第{}行: 用户名不能为空，跳过此行", i+1);
                    continue;
                }
                
                // 检查用户是否已存在
                boolean userExists = existingUsers.stream()
                        .anyMatch(u -> username.equals(u.getUsername()) || 
                                      (email != null && email.equals(u.getEmail())));
                
                if (userExists) {
                    log.warn("第{}行: 用户 '{}' 已存在，跳过导入", i+1, username);
                    continue;
                }
                
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPhoneNumber(phoneNumber);
                user.setPrimaryCompanyId(companyId);
                user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv"); // 默认密码
                
                // 保存用户
                User savedUser = userRepository.save(user);
                
                // 如果指定了部门，则建立用户部门关联
                if (departmentName != null && !departmentName.trim().isEmpty()) {
                    Long departmentId = departmentNameToIdMap.get(departmentName);
                    if (departmentId == null) {
                        log.warn("第{}行: 找不到部门 '{}'", i+1, departmentName);
                    } else {
                        UserDepartment userDepartment = new UserDepartment(savedUser.getUserId(), departmentId);
                        userDepartmentRepository.save(userDepartment);
                    }
                }
                
                log.info("成功导入用户: {}", username);
            }
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