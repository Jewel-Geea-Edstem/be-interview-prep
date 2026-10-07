package com.example.prep.task.service;

import com.example.prep.common.exception.NotFoundException;
import com.example.prep.task.dto.request.TaskRequest;
import com.example.prep.task.dto.response.TaskResponse;
import com.example.prep.task.entity.Task;
import com.example.prep.task.entity.TaskStatus;
import com.example.prep.task.mapper.TaskMapper;
import com.example.prep.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskService {

  private final TaskRepository taskRepository;
  private final TaskMapper taskMapper;

  @Transactional
  public TaskResponse create(TaskRequest request) {
    Task task = new Task();
    taskMapper.apply(request, task);
    return taskMapper.toResponse(taskRepository.save(task));
  }

  @Transactional(readOnly = true)
  public Page<TaskResponse> list(TaskStatus status, Pageable pageable) {
    Page<Task> page =
        status == null
            ? taskRepository.findAll(pageable)
            : taskRepository.findByStatus(status, pageable);
    return page.map(taskMapper::toResponse);
  }

  @Transactional(readOnly = true)
  public TaskResponse get(Long id) {
    return taskMapper.toResponse(find(id));
  }

  @Transactional
  public TaskResponse update(Long id, TaskRequest request) {
    Task task = find(id);
    taskMapper.apply(request, task);
    return taskMapper.toResponse(task);
  }

  @Transactional
  public void delete(Long id) {
    taskRepository.delete(find(id));
  }

  private Task find(Long id) {
    return taskRepository.findById(id).orElseThrow(() -> new NotFoundException("Task", id));
  }
}
