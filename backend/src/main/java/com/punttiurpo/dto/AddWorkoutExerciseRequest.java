package com.punttiurpo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddWorkoutExerciseRequest(
        @NotNull(message = "Liikkeen tunniste on pakollinen.")
        @Positive(message = "Liikkeen tunnisteen on oltava positiivinen.") Long exerciseId,
        @Size(max = 2000, message = "Muistiinpanot saavat olla enintään 2000 merkkiä.") String notes) {
    public AddWorkoutExerciseRequest {
        notes = notes == null || notes.isBlank() ? null : notes.strip();
    }
}
