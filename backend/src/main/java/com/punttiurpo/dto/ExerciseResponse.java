package com.punttiurpo.dto;

public record ExerciseResponse(Long id, String name, String category,
                               String primaryMuscleGroup, String equipment,
                               String notes, boolean custom) {
}
