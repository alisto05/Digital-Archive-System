package com.syncpoint.archive.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        if (body == null && ex instanceof ErrorResponse errorResponse) {
            body = errorResponse.getBody();
        }

        String message = null;
        if (body instanceof ProblemDetail problem && problem.getDetail() != null) {
            message = problem.getDetail();
        }
        if (message == null) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            message = status != null ? status.getReasonPhrase() : "Request failed.";
        }

        Object payload = Map.of("error", message);
        return ResponseEntity.status(statusCode).headers(headers).body(payload);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.put(fe.getField(), fe.getDefaultMessage()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);
        Object payload = body;
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(payload);
    }

    

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateKeyException ex) {
        return error(HttpStatus.CONFLICT, "That value is already in use.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return error(HttpStatus.BAD_REQUEST, "The request refers to a record that does not exist or contains invalid data.");
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<Map<String, String>> handleEmpty(EmptyResultDataAccessException ex) {
        return error(HttpStatus.NOT_FOUND, "Record not found.");
    }

    
    @ExceptionHandler(UncategorizedSQLException.class)
    public ResponseEntity<Map<String, String>> handleProcedureError(UncategorizedSQLException ex) {
        SQLException sql = ex.getSQLException();
        if (sql != null && "45000".equals(sql.getSQLState()) && sql.getMessage() != null) {
            String message = sql.getMessage();
            boolean duplicate = message.toLowerCase(Locale.ROOT).contains("already")
                    || message.toLowerCase(Locale.ROOT).contains("duplicate");
            return error(duplicate ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST, message);
        }
        return handleDatabase(ex);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDatabase(DataAccessException ex) {
        log.error("Database error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred.");
    }

    

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIo(IOException ex) {
        log.error("I/O error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "The file could not be processed.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong.");
    }

    private static ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message != null ? message : status.getReasonPhrase()));
    }
}
