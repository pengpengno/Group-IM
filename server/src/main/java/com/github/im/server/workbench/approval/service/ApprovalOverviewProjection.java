package com.github.im.server.workbench.approval.service;
import com.github.im.dto.workbench.overview.WorkbenchApprovalSummaryDTO;
import java.util.List;
public record ApprovalOverviewProjection(long pendingCount,List<WorkbenchApprovalSummaryDTO> recent) { }
