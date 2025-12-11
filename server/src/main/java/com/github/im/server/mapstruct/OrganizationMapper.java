package com.github.im.server.mapstruct;

import com.github.im.dto.organization.CompanyDTO;
import com.github.im.dto.organization.DepartmentDTO;
import com.github.im.server.model.Company;
import com.github.im.server.model.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {
    OrganizationMapper INSTANCE = Mappers.getMapper(OrganizationMapper.class);

    @Mapping(target = "code", source = "schemaName")
    CompanyDTO companyToCompanyDTO(Company company);
    
    @Mapping(target = "schemaName", source = "code")
    Company companyDTOToCompany(CompanyDTO companyDTO);
    
    List<CompanyDTO> companiesToCompanyDTOs(List<Company> companies);
    
    DepartmentDTO departmentToDepartmentDTO(Department department);
    
    List<DepartmentDTO> departmentsToDepartmentDTOs(List<Department> departments);
}