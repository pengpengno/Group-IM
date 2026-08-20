package com.github.im.server.workbench.overview;

import com.github.im.dto.workbench.overview.WorkbenchOverviewDTO;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.workbench.common.context.CurrentWorkContext;
import com.github.im.server.workbench.common.permission.WorkbenchPermission;
import com.github.im.server.workbench.common.permission.WorkbenchPermissionService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        WorkbenchOverviewService service = new WorkbenchOverviewService(
                permissionService,
                repository,
                Clock.systemUTC()
        );

        ApiResponse<WorkbenchOverviewDTO> response = new WorkbenchOverviewController(service).overview();

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        assertEquals(7L, response.getData().currentCompany().companyId());
    }
}
