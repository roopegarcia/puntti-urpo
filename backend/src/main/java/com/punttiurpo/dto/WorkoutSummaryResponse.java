package com.punttiurpo.dto;

import java.time.Instant;
import java.time.LocalDate;

public record WorkoutSummaryResponse(Long id, LocalDate date, String title, String notes,
                                     Instant startedAt, Instant endedAt) {
}
