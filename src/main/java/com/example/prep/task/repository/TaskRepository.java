package com.example.prep.task.repository;

import com.example.prep.task.entity.Task;
import com.example.prep.task.entity.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

  Page<Task> findByStatus(TaskStatus status, Pageable pageable);
}
