package com.punttiurpo.controller;

import java.net.URI;
import com.punttiurpo.dto.AddWorkoutExerciseRequest;
import com.punttiurpo.dto.WorkoutExerciseResponse;
import com.punttiurpo.dto.WorkoutPage;
import com.punttiurpo.dto.WorkoutRequest;
import com.punttiurpo.dto.WorkoutResponse;
import com.punttiurpo.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workouts")
@Tag(name = "Workouts", description = "Local workout logging; authentication is not implemented yet.")
public class WorkoutController {
    private final WorkoutService service;

    public WorkoutController(WorkoutService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List workout summaries, newest training date first")
    public WorkoutPage list(@RequestParam(defaultValue = "0") @Min(0) @Max(1_000_000) int page,
                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(page, size);
    }

    @PostMapping
    @Operation(summary = "Create a workout")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<WorkoutResponse> create(@Valid @RequestBody WorkoutRequest request) {
        var result = service.create(request);
        return ResponseEntity.created(URI.create("/api/workouts/" + result.id())).body(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read a workout including ordered exercises and sets")
    public WorkoutResponse get(@PathVariable @Positive long id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace workout metadata; exercises and sets are preserved")
    public WorkoutResponse update(@PathVariable @Positive long id, @Valid @RequestBody WorkoutRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a workout and its logged exercises and sets")
    @ApiResponse(responseCode = "204", description = "No content")
    public ResponseEntity<Void> delete(@PathVariable @Positive long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/exercises")
    @Operation(summary = "Append an exercise to a workout")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<WorkoutExerciseResponse> addExercise(@PathVariable @Positive long id,
            @Valid @RequestBody AddWorkoutExerciseRequest request) {
        var result = service.addExercise(id, request);
        return ResponseEntity.created(URI.create("/api/workout-exercises/" + result.id())).body(result);
    }
}
