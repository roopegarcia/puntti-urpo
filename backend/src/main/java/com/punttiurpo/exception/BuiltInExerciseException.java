package com.punttiurpo.exception;

public class BuiltInExerciseException extends RuntimeException {
    public BuiltInExerciseException() {
        super("Vain omia liikkeitä voi muokata.");
    }
}
