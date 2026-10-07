package com.example.prep.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.prep.common.exception.NotFoundException;
import com.example.prep.task.dto.request.TaskRequest;
import com.example.prep.task.dto.response.TaskResponse;
import com.example.prep.task.entity.Task;
import com.example.prep.task.entity.TaskStatus;
import com.example.prep.task.mapper.TaskMapper;
import com.example.prep.task.repository.TaskRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  private TaskService taskService;

  @BeforeEach
  void setUp() {
    taskService = new TaskService(taskRepository, new TaskMapper());
  }

  @Test
  void createDefaultsStatusToTodoAndTrimsTitle() {
    TaskRequest request = new TaskRequest("  Plan sprint  ", null, null, null);
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response = taskService.create(request);

    assertThat(response.status()).isEqualTo(TaskStatus.TODO);
    assertThat(response.title()).isEqualTo("Plan sprint");
  }

  @Test
  void deleteOfUnknownTaskThrowsNotFound() {
    when(taskRepository.findById(42L)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> taskService.delete(42L));

    verify(taskRepository, never()).delete(any(Task.class));
  }
}
