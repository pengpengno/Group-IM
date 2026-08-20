package com.github.im.server.workbench.task.repository;

import com.github.im.server.workbench.task.model.TaskAssigneeRole;
import com.github.im.server.workbench.task.model.TaskStatus;
import com.github.im.server.workbench.task.model.WorkTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface WorkTaskRepository extends JpaRepository<WorkTask, Long> {

    @Query("""
            SELECT t
            FROM WorkTask t
            WHERE t.deleted = false
              AND (
                    t.creatorId = :userId
                    OR t.ownerId = :userId
                    OR EXISTS (
                        SELECT a.assigneeId
                        FROM WorkTaskAssignee a
                        WHERE a.taskId = t.taskId
                          AND a.userId = :userId
                    )
              )
            ORDER BY t.updatedAt DESC, t.taskId DESC
            """)
    List<WorkTask> findVisibleToUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT COUNT(t)
            FROM WorkTask t
            WHERE t.deleted = false
              AND t.status NOT IN :terminalStatuses
              AND EXISTS (
                    SELECT a.assigneeId
                    FROM WorkTaskAssignee a
                    WHERE a.taskId = t.taskId
                      AND a.userId = :userId
                      AND a.role IN :roles
              )
            """)
    long countAssignedOpen(
            @Param("userId") Long userId,
            @Param("roles") Collection<TaskAssigneeRole> roles,
            @Param("terminalStatuses") Collection<TaskStatus> terminalStatuses
    );

    @Query("""
            SELECT COUNT(t)
            FROM WorkTask t
            WHERE t.deleted = false
              AND t.dueAt IS NOT NULL
              AND t.dueAt < :now
              AND t.status NOT IN :terminalStatuses
              AND EXISTS (
                    SELECT a.assigneeId
                    FROM WorkTaskAssignee a
                    WHERE a.taskId = t.taskId
                      AND a.userId = :userId
                      AND a.role IN :roles
              )
            """)
    long countAssignedOverdue(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now,
            @Param("roles") Collection<TaskAssigneeRole> roles,
            @Param("terminalStatuses") Collection<TaskStatus> terminalStatuses
    );
}
