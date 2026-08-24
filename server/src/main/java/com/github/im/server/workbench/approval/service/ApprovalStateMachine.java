package com.github.im.server.workbench.approval.service;

import com.github.im.server.workbench.approval.model.ApprovalActionType;
import com.github.im.server.workbench.approval.model.ApprovalStatus;
import com.github.im.server.workbench.common.error.WorkbenchErrorCode;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.springframework.stereotype.Component;

@Component
public class ApprovalStateMachine {
    public void require(ApprovalStatus status, ApprovalActionType action) {
        boolean allowed = switch (action) {
            case SUBMIT -> status == ApprovalStatus.DRAFT;
            case APPROVE, REJECT, RETURN -> status == ApprovalStatus.PENDING;
            case RESUBMIT -> status == ApprovalStatus.RETURNED;
            case CANCEL -> status == ApprovalStatus.DRAFT || status == ApprovalStatus.PENDING || status == ApprovalStatus.RETURNED;
        };
        if (!allowed) {
            throw WorkbenchException.conflict(WorkbenchErrorCode.APPROVAL_INVALID_TRANSITION,
                    "审批状态 " + status + " 不允许执行 " + action);
        }
    }
}
