package com.github.im.server.workbench.task.repository;

import com.github.im.server.workbench.task.model.WorkTaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkTaskActivityRepository extends JpaRepository<WorkTaskActivity, Long> {

    List<WorkTaskActivity> findByTaskIdOrderByCreatedAtAscActivityIdAsc(Long taskId);
}
