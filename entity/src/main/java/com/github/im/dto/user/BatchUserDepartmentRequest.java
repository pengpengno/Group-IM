package com.github.im.dto.user;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量用户部门分配请求对象
 */
@Data
public class BatchUserDepartmentRequest {
    @NotEmpty(message = "需要分配的用户Id不能为空！")
    private List<Long> userIds;
}