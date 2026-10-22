package com.punttiurpo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExerciseRequest(
        @NotBlank(message = "Nimi on pakollinen.")
        @Size(max = 100, message = "Nimi saa olla enintään 100 merkkiä.") String name,
        @Size(max = 50, message = "Luokka saa olla enintään 50 merkkiä.") String category,
        @Size(max = 50, message = "Lihasryhmä saa olla enintään 50 merkkiä.") String primaryMuscleGroup,
        @Size(max = 50, message = "Väline saa olla enintään 50 merkkiä.") String equipment,
        @Size(max = 2000, message = "Muistiinpanot saavat olla enintään 2000 merkkiä.") String notes) {

    public ExerciseRequest {
        name = normalize(name);
        category = normalize(category);
        primaryMuscleGroup = normalize(primaryMuscleGroup);
        equipment = normalize(equipment);
        notes = normalize(notes);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
