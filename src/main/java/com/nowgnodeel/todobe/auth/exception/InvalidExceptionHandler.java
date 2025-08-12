package com.nowgnodeel.todobe.auth.exception;

import com.nowgnodeel.todobe.auth.dto.UserVerificationResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class InvalidExceptionHandler {
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<UserVerificationResponseDto> handleInvalidPasswordException(InvalidPasswordException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new UserVerificationResponseDto(e.getMessage()));
    }
}
