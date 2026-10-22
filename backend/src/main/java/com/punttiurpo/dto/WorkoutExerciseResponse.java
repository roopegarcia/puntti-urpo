package com.punttiurpo.dto;

import java.util.List;

public record WorkoutExerciseResponse(Long id, ExerciseResponse exercise, int displayOrder,
                                      String notes, List<WorkoutSetResponse> sets) {
}
