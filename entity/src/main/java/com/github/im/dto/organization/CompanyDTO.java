package com.github.im.dto.organization;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CompanyDTO implements Serializable {
    private Long companyId;
    @NotBlank(message = "名称不能为空")
    private String name;
    @NotBlank(message = "代码不能为空")
    private String code;
}