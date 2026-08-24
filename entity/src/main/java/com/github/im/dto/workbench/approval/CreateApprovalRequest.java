package com.github.im.dto.workbench.approval;
import java.util.List;
public record CreateApprovalRequest(Long definitionId, String title, Long departmentId,
                                    String formDataJson, List<Long> approverIds, List<Long> ccUserIds) { }
