package com.github.im.dto.workbench.overview;

import java.time.LocalDateTime;

public record WorkbenchAnnouncementSummaryDTO(Long announcementId, String title, String priority, LocalDateTime publishedAt) {
}
