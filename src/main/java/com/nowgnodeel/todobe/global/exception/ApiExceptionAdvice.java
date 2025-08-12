package com.nowgnodeel.todobe.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionAdvice {

    @ExceptionHandler({ApiException.class})
    public ResponseEntity<String> exceptionHandler(HttpServletRequest httpServletRequest, ApiException apiException) {
        return ResponseEntity
                .status(apiException.getHttpStatus())
                .body(apiException.getMessage());
    }
}
