package com.example.prep.task.controller;

import com.example.prep.task.dto.request.TaskRequest;
import com.example.prep.task.dto.response.TaskResponse;
import com.example.prep.task.entity.TaskStatus;
import com.example.prep.task.service.TaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

  private final TaskService taskService;

  @PostMapping
  public ResponseEntity<TaskResponse> create(@Valid @RequestBody TaskRequest request) {
    TaskResponse created = taskService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
  }

  @GetMapping
  public PagedModel<TaskResponse> list(
      @RequestParam(required = false) TaskStatus status,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return new PagedModel<>(taskService.list(status, pageable));
  }

  @GetMapping("/{id}")
  public TaskResponse get(@PathVariable @Positive Long id) {
    return taskService.get(id);
  }

  @PutMapping("/{id}")
  public TaskResponse update(
      @PathVariable @Positive Long id, @Valid @RequestBody TaskRequest request) {
    return taskService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
    taskService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
