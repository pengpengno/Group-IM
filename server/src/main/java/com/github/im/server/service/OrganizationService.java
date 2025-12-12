package com.github.im.server.service;

import com.github.im.server.model.Department;
import com.github.im.server.model.User;
import com.github.im.server.repository.DepartmentRepository;
import com.github.im.server.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class OrganizationService {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取指定公司的组织架构
     * @param companyId 公司ID
     * @return 组织架构树
     */
    public List<Department> getOrganizationStructure(Long companyId) {
        // 获取公司所有部门
        List<Department> allDepartments = departmentRepository.findByCompanyIdAndStatusTrue(companyId);

        // 构建部门ID到部门对象的映射
        Map<Long, Department> departmentMap = allDepartments.stream()
                .collect(Collectors.toMap(Department::getDepartmentId, dept -> dept));

        // 构建树形结构
        List<Department> rootDepartments = new ArrayList<>();
        for (Department dept : allDepartments) {
            Long parentId = dept.getParentId();
            if (parentId == null) {
                // 顶级部门
                rootDepartments.add(dept);
            } else {
                // 子部门
                Department parentDept = departmentMap.get(parentId);
                if (parentDept != null) {
                    if (parentDept.getChildren() == null) {
                        parentDept.setChildren(new ArrayList<>());
                    }
                    parentDept.getChildren().add(dept);
                }
            }
        }

        // 为每个部门加载成员
        loadMembersForDepartments(allDepartments, companyId);

        return rootDepartments;
    }

    /**
     * 为部门加载成员
     * @param departments 部门列表
     * @param companyId 公司ID
     */
    private void loadMembersForDepartments(List<Department> departments, Long companyId) {
        // 获取公司所有用户
        List<User> companyUsers = userRepository.findByPrimaryCompanyId(companyId);

        // 按部门分组用户
        Map<Long, List<User>> usersByDepartment = companyUsers.stream()
                .filter(user -> user.getDepartmentId() != null)
                .collect(Collectors.groupingBy(User::getDepartmentId));

        // 将用户分配给对应的部门
        for (Department department : departments) {
            List<User> members = usersByDepartment.getOrDefault(department.getDepartmentId(), new ArrayList<>());
            department.setMembers(members);
        }
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
        // 这里应该实现具体的Excel解析和部门数据导入逻辑
        // 为简化起见，此处仅给出框架代码
        InputStream inputStream = file.getInputStream();
        // 解析Excel文件
        // 验证数据格式
        // 保存到数据库
    }
    
    /**
     * 导入员工数据
     * @param file Excel文件
     * @param companyId 公司ID
     */
    public void importEmployees(MultipartFile file, Long companyId) throws Exception {
        // 这里应该实现具体的Excel解析和员工数据导入逻辑
        // 为简化起见，此处仅给出框架代码
        InputStream inputStream = file.getInputStream();
        // 解析Excel文件
        // 验证数据格式
        // 保存到数据库
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
        headerRow.createCell(0).setCellValue("部门名称(name)");
        headerRow.createCell(1).setCellValue("父部门ID(parentId)");
        headerRow.createCell(2).setCellValue("部门描述(description)");
        
        // 调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        
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
        headerRow.createCell(0).setCellValue("用户名(username)");
        headerRow.createCell(1).setCellValue("邮箱(email)");
        headerRow.createCell(2).setCellValue("手机号(phoneNumber)");
        headerRow.createCell(3).setCellValue("部门ID(departmentId)");
        
        // 调整列宽
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        
        // 写入输出流
        workbook.write(outputStream);
        workbook.close();
    }
}