package com.punttiurpo.dto;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkoutRequest(
        @NotNull(message = "Päivämäärä on pakollinen.") LocalDate date,
        @Size(max = 100, message = "Otsikko saa olla enintään 100 merkkiä.") String title,
        @Size(max = 2000, message = "Muistiinpanot saavat olla enintään 2000 merkkiä.") String notes,
        Instant startedAt, Instant endedAt) {
    public WorkoutRequest {
        title = normalize(title);
        notes = normalize(notes);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
