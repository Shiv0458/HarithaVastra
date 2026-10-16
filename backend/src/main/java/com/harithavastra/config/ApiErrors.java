package com.harithavastra.config;

import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
      Map.of("message", Objects.toString(e.getReason(), "Request failed"))
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(
      Map.of(
        "message",
        "Please check the form",
        "fields",
        e
          .getBindingResult()
          .getFieldErrors()
          .stream()
          .map(f ->
            Map.of(
              "field",
              f.getField(),
              "message",
              Objects.toString(f.getDefaultMessage(), "Invalid")
            )
          )
          .toList()
      )
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> duplicate() {
    return ResponseEntity.status(409).body(
      Map.of(
        "message",
        "This record already exists or conflicts with current data. Refresh and try again."
      )
    );
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
  })
  ResponseEntity<?> invalid() {
    return ResponseEntity.badRequest().body(
      Map.of("message", "Invalid request format")
    );
  }

  @ExceptionHandler(
    org.springframework.security.access.AccessDeniedException.class
  )
  ResponseEntity<?> denied() {
    return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error(
      "Request failed: {}",
      e.getClass().getSimpleName()
    );
    return ResponseEntity.status(500).body(
      Map.of("message", "We could not complete this request. Please retry.")
    );
  }
}
