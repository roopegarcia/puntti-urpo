package com.punttiurpo.controller;

import java.net.URI;
import com.punttiurpo.dto.WorkoutExerciseRequest;
import com.punttiurpo.dto.WorkoutExerciseResponse;
import com.punttiurpo.dto.WorkoutSetRequest;
import com.punttiurpo.dto.WorkoutSetResponse;
import com.punttiurpo.service.WorkoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Workout entries")
public class WorkoutEntryController {
    private final WorkoutService service;

    public WorkoutEntryController(WorkoutService service) {
        this.service = service;
    }

    @GetMapping("/workout-exercises/{id}")
    @Operation(summary = "Read a logged exercise and its ordered sets")
    public WorkoutExerciseResponse getExercise(@PathVariable @Positive long id) {
        return service.getExercise(id);
    }

    @GetMapping("/sets/{id}")
    @Operation(summary = "Read a logged set")
    public WorkoutSetResponse getSet(@PathVariable @Positive long id) {
        return service.getSet(id);
    }

    @PutMapping("/workout-exercises/{id}")
    @Operation(summary = "Replace logged-exercise notes and order; catalogue reference stays fixed")
    public WorkoutExerciseResponse updateExercise(@PathVariable @Positive long id,
            @Valid @RequestBody WorkoutExerciseRequest request) {
        return service.updateExercise(id, request);
    }

    @DeleteMapping("/workout-exercises/{id}")
    @Operation(summary = "Remove a logged exercise and its sets; keep the catalogue exercise")
    @ApiResponse(responseCode = "204", description = "No content")
    public ResponseEntity<Void> deleteExercise(@PathVariable @Positive long id) {
        service.deleteExercise(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/workout-exercises/{id}/sets")
    @Operation(summary = "Add a draft or completed set; omitted order appends")
    @ApiResponse(responseCode = "201", description = "Created")
    public ResponseEntity<WorkoutSetResponse> addSet(@PathVariable @Positive long id,
            @Valid @RequestBody WorkoutSetRequest request) {
        var result = service.addSet(id, request);
        return ResponseEntity.created(URI.create("/api/sets/" + result.id())).body(result);
    }

    @PutMapping("/sets/{id}")
    @Operation(summary = "Replace a set; omitted order preserves its current order")
    public WorkoutSetResponse updateSet(@PathVariable @Positive long id,
            @Valid @RequestBody WorkoutSetRequest request) {
        return service.updateSet(id, request);
    }

    @DeleteMapping("/sets/{id}")
    @Operation(summary = "Delete one logged set")
    @ApiResponse(responseCode = "204", description = "No content")
    public ResponseEntity<Void> deleteSet(@PathVariable @Positive long id) {
        service.deleteSet(id);
        return ResponseEntity.noContent().build();
    }
}
