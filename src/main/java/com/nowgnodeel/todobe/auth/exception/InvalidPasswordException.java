package com.nowgnodeel.todobe.auth.exception;

public class InvalidPasswordException extends IllegalArgumentException {
    public InvalidPasswordException(String msg) {
        super(msg);
    }
}
