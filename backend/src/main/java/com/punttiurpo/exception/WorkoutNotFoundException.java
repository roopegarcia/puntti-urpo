package com.punttiurpo.exception;

public class WorkoutNotFoundException extends RuntimeException {
    public WorkoutNotFoundException() {
        super("Harjoitusta, harjoitusliikettä tai sarjaa ei löytynyt.");
    }
}
