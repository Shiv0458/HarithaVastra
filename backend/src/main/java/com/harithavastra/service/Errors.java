package com.harithavastra.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class Errors {

  public static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  public static ResponseStatusException conflict(String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  public static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
  }

  public static ResponseStatusException unauthorized() {
    return new ResponseStatusException(
      HttpStatus.UNAUTHORIZED,
      "Please sign in again"
    );
  }
}
