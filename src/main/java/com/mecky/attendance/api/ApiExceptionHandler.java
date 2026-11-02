package com.mecky.attendance.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import java.util.*;
@RestControllerAdvice public class ApiExceptionHandler {
 @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class) public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed":e.getReason()));}
 @ExceptionHandler(NoSuchElementException.class) public ResponseEntity<?> missing(Exception e){return ResponseEntity.status(404).body(Map.of("error",e.getMessage()==null?"Not found":e.getMessage()));}
 @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("error","Record conflicts with existing data"));}
 @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class) public ResponseEntity<?> validation(Exception e){return ResponseEntity.badRequest().body(Map.of("error","Request validation failed"));}
 @ExceptionHandler(Exception.class) public ResponseEntity<?> bad(Exception e){return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()==null?"Request failed":e.getMessage()));}
}
