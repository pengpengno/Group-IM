package com.github.im.server.workbench.task.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "wb_task_activity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long activityId;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TaskActivityAction action;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TaskStatus beforeState;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private TaskStatus afterState;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
