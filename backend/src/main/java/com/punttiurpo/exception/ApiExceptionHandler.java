package com.punttiurpo.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    public record FieldError(String field, String message) {
    }

    @ExceptionHandler(ExerciseNotFoundException.class)
    ProblemDetail notFound(ExerciseNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Liikettä ei löytynyt", exception.getMessage());
    }

    @ExceptionHandler(BuiltInExerciseException.class)
    ProblemDetail forbidden(BuiltInExerciseException exception) {
        return problem(HttpStatus.FORBIDDEN, "Muokkaus estetty", exception.getMessage());
    }

    @ExceptionHandler(WorkoutNotFoundException.class)
    ProblemDetail workoutNotFound(WorkoutNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, "Harjoitustietoa ei löytynyt", exception.getMessage());
    }

    @ExceptionHandler(InvalidWorkoutException.class)
    ProblemDetail invalidWorkout(InvalidWorkoutException exception) {
        return problem(HttpStatus.BAD_REQUEST, "Virheellinen harjoitustieto", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict() {
        return problem(HttpStatus.CONFLICT, "Tallennusristiriita",
                "Tallennus rikkoo tietokannan rajoitetta. Tarkista nimi, järjestysnumero ja viittaukset.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception) {
        log.error("Unexpected API failure", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Palvelinvirhe",
                "Pyyntöä ei voitu käsitellä.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = invalidRequest();
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage())).toList();
        body.setProperty("errors", errors);
        return handleExceptionInternal(exception, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, invalidRequest(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, invalidRequest(), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, invalidRequest(), headers, status, request);
    }

    private static ProblemDetail invalidRequest() {
        return problem(HttpStatus.BAD_REQUEST, "Virheellinen pyyntö",
                "Tarkista pyynnön kentät, tunniste ja sivutusparametrit.");
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
