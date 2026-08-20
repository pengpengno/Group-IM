package com.github.im.server.workbench.task.repository;

import com.github.im.server.workbench.task.model.WorkTaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkTaskCommentRepository extends JpaRepository<WorkTaskComment, Long> {

    List<WorkTaskComment> findByTaskIdAndDeletedFalseOrderByCreatedAtAsc(Long taskId);

    Optional<WorkTaskComment> findByCommentIdAndTaskIdAndDeletedFalse(Long commentId, Long taskId);
}
