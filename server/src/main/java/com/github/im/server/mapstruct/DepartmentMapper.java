package com.github.im.server.mapstruct;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",uses = {UserMapper.class})
public interface DepartmentMapper {

    DepartmentDTO departmentToDepartmentDTO(Department department);

    /***
     * 批量转换
     * @param departments
     * @return 列表
     */
    List<DepartmentDTO> departmentsToDepartmentDTOs(List<Department> departments);


}