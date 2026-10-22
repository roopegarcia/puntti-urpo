package com.punttiurpo.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record WorkoutResponse(Long id, LocalDate date, String title, String notes,
                              Instant startedAt, Instant endedAt,
                              List<WorkoutExerciseResponse> exercises) {
}
