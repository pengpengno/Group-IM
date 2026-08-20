package com.github.im.server.workbench.overview;

import com.github.im.dto.workbench.overview.WorkbenchOverviewDTO;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import com.github.im.server.workbench.task.service.TaskOverviewProjection;
import com.github.im.server.workbench.task.service.TaskOverviewQueryService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkbenchOverviewControllerTest {

    @Test
    void returnsProjectStandardApiResponse() {
        CurrentWorkContext context = new CurrentWorkContext(42L, "alice", 7L, "Acme", "tenant_a");
        WorkbenchPermissionService permissionService = new WorkbenchPermissionService() {
            @Override
            public CurrentWorkContext require(WorkbenchPermission permission) {
                return context;
            }

            @Override
            public boolean isAllowed(WorkbenchPermission permission) {
                return true;
            }
        };
        MeetingOverviewRepository repository =
                (userId, participantStatuses, meetingStatuses, start, end) -> List.of();
        TaskOverviewQueryService taskQuery = mock(TaskOverviewQueryService.class);
        when(taskQuery.query(eq(42L), any(LocalDateTime.class)))
                .thenReturn(new TaskOverviewProjection(0, 0, List.of()));
        WorkbenchOverviewService service = new WorkbenchOverviewService(
                permissionService,
                repository,
                taskQuery,
                Clock.systemUTC()
        );

        ApiResponse<WorkbenchOverviewDTO> response = new WorkbenchOverviewController(service).overview();

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(7L, response.getData().currentCompany().companyId());
    }
}
