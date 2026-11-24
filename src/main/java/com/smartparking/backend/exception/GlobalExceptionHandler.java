
package com.smartparking.backend.exception;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {

        int status = ex.getStatusCode().value();

        HttpStatus httpStatus = HttpStatus.resolve(status);

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status);
        body.put("error",
                httpStatus != null
                        ? httpStatus.getReasonPhrase()
                        : "Error");
        body.put("message", ex.getReason());
        body.put("path", request.getRequestURI());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(body);
    }
}
