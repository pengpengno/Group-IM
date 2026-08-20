package com.github.im.server.workbench.overview;

import com.github.im.dto.workbench.overview.WorkbenchOverviewDTO;
import com.github.im.server.web.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workbench")
public class WorkbenchOverviewController {

    private final WorkbenchOverviewService overviewService;

    public WorkbenchOverviewController(WorkbenchOverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    public ApiResponse<WorkbenchOverviewDTO> overview() {
        return ApiResponse.success(overviewService.getOverview());
    }
}
