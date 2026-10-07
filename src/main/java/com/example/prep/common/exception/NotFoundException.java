package com.example.prep.common.exception;

public class NotFoundException extends RuntimeException {

  public NotFoundException(String resource, Object id) {
    super(resource + " " + id + " not found");
  }
}
