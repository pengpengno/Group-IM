package com.github.im.dto.organization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.im.dto.user.UserBasicInfo;
import com.github.im.dto.user.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mapstruct.Mapping;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DepartmentDTO implements Serializable {
    private Long departmentId;  // 根节点  是  公司 设为 空即可
    private String name;
    private String description;
    private Long companyId;
    private Long parentId;
    private Integer orderNum;
    private Boolean status;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<DepartmentDTO> children;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<UserBasicInfo> members;
}