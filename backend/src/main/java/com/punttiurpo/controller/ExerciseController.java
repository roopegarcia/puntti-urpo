package com.punttiurpo.controller;

import java.net.URI;

import com.punttiurpo.dto.ExercisePage;
import com.punttiurpo.dto.ExerciseRequest;
import com.punttiurpo.dto.ExerciseResponse;
import com.punttiurpo.service.ExerciseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exercises")
@Tag(name = "Exercises", description = "Local exercise catalogue; authentication is not implemented yet.")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Browse exercises or search names; pages are zero-based")
    public ExercisePage list(
            @RequestParam(defaultValue = "") @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(q, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read an exercise")
    public ExerciseResponse get(@PathVariable @Positive long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a custom exercise")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<ExerciseResponse> create(@Valid @RequestBody ExerciseRequest request) {
        ExerciseResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/exercises/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a custom exercise; omitted optional fields become null")
    public ExerciseResponse update(@PathVariable @Positive long id,
                                   @Valid @RequestBody ExerciseRequest request) {
        return service.update(id, request);
    }
}
