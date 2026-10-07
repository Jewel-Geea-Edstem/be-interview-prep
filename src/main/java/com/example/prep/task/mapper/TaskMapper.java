package com.example.prep.task.mapper;

import com.example.prep.task.dto.request.TaskRequest;
import com.example.prep.task.dto.response.TaskResponse;
import com.example.prep.task.entity.Task;
import com.example.prep.task.entity.TaskStatus;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

  public TaskResponse toResponse(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDescription(),
        task.getStatus(),
        task.getDueDate(),
        task.getCreatedAt());
  }

  public void apply(TaskRequest request, Task task) {
    task.setTitle(request.title().strip());
    task.setDescription(request.description());
    task.setStatus(request.status() == null ? TaskStatus.TODO : request.status());
    task.setDueDate(request.dueDate());
  }
}
