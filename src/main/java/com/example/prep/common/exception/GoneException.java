package com.example.prep.common.exception;

public class GoneException extends RuntimeException {

  public GoneException(String resource, Object id) {
    super(resource + " " + id + " has expired");
  }
}
