package com.github.im.server.workbench.approval.service;

import com.github.im.server.workbench.approval.model.*;
import com.github.im.server.workbench.common.error.WorkbenchException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalStateMachineTest {
    private final ApprovalStateMachine machine=new ApprovalStateMachine();

    @Test void supportsV1Transitions() {
        assertDoesNotThrow(()->machine.require(ApprovalStatus.DRAFT,ApprovalActionType.SUBMIT));
        assertDoesNotThrow(()->machine.require(ApprovalStatus.PENDING,ApprovalActionType.APPROVE));
        assertDoesNotThrow(()->machine.require(ApprovalStatus.PENDING,ApprovalActionType.REJECT));
        assertDoesNotThrow(()->machine.require(ApprovalStatus.PENDING,ApprovalActionType.RETURN));
        assertDoesNotThrow(()->machine.require(ApprovalStatus.RETURNED,ApprovalActionType.RESUBMIT));
        assertDoesNotThrow(()->machine.require(ApprovalStatus.PENDING,ApprovalActionType.CANCEL));
    }

    @Test void terminalStatesFailClosed() {
        assertThrows(WorkbenchException.class,()->machine.require(ApprovalStatus.APPROVED,ApprovalActionType.APPROVE));
        assertThrows(WorkbenchException.class,()->machine.require(ApprovalStatus.REJECTED,ApprovalActionType.RESUBMIT));
        assertThrows(WorkbenchException.class,()->machine.require(ApprovalStatus.CANCELLED,ApprovalActionType.SUBMIT));
    }
}
