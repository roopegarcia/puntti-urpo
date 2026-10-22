package com.punttiurpo.exception;

public class ExerciseNotFoundException extends RuntimeException {
    public ExerciseNotFoundException() {
        super("Liikettä ei löytynyt.");
    }
}
