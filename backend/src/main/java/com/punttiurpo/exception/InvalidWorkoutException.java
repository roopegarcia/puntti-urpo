package com.punttiurpo.exception;

public class InvalidWorkoutException extends RuntimeException {
    public InvalidWorkoutException(String message) {
        super(message);
    }
}
