package com.example.prep.task.dto.response;

import com.example.prep.task.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
    Long id,
    String title,
    String description,
    TaskStatus status,
    LocalDate dueDate,
    Instant createdAt) {}
