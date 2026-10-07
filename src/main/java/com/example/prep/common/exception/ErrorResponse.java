package com.example.prep.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp,
    List<FieldViolation> details) {

  public record FieldViolation(String field, String message) {}

  public static ErrorResponse of(
      int status, String error, String message, String path, List<FieldViolation> details) {
    return new ErrorResponse(status, error, message, path, Instant.now(), details);
  }
}
