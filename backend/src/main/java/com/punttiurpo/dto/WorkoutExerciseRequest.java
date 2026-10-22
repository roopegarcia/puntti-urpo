package com.punttiurpo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutExerciseRequest(
        @NotNull(message = "Järjestysnumero on pakollinen.")
        @Min(value = 0, message = "Järjestysnumeron on oltava vähintään 0.")
        @Max(value = 1_000_000, message = "Järjestysnumero on liian suuri.") Integer displayOrder,
        @Size(max = 2000, message = "Muistiinpanot saavat olla enintään 2000 merkkiä.") String notes) {
    public WorkoutExerciseRequest {
        notes = notes == null || notes.isBlank() ? null : notes.strip();
    }
}
