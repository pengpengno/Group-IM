package com.github.im.server.workbench.approval.service;
import com.github.im.dto.workbench.overview.WorkbenchApprovalSummaryDTO;
import com.github.im.server.workbench.approval.model.*;
import com.github.im.server.workbench.approval.repository.*;
import org.springframework.stereotype.Service;

@Service
public class ApprovalOverviewQueryService {
    private final ApprovalNodeRepository nodes;
    private final ApprovalInstanceRepository instances;
    public ApprovalOverviewQueryService(ApprovalNodeRepository nodes,ApprovalInstanceRepository instances) { this.nodes=nodes; this.instances=instances; }
    public ApprovalOverviewProjection query(Long userId) {
        var pending=nodes.findByAssigneeIdAndStatusOrderByStartedAtDesc(userId,ApprovalNodeStatus.PENDING);
        var recent=pending.stream().limit(5).map(ApprovalNode::getInstanceId).map(instances::findById)
                .flatMap(java.util.Optional::stream)
                .map(value->new WorkbenchApprovalSummaryDTO(value.getInstanceId(),value.getTitle(),value.getStatus().name(),value.getSubmittedAt()))
                .toList();
        return new ApprovalOverviewProjection(nodes.countByAssigneeIdAndStatus(userId,ApprovalNodeStatus.PENDING),recent);
    }
}
