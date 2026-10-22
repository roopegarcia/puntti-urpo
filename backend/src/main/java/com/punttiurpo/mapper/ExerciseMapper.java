package com.punttiurpo.mapper;

import com.punttiurpo.dto.ExerciseResponse;
import com.punttiurpo.entity.Exercise;

public final class ExerciseMapper {
    private ExerciseMapper() {
    }

    public static ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(exercise.getId(), exercise.getName(), exercise.getCategory(),
                exercise.getPrimaryMuscleGroup(), exercise.getEquipment(),
                exercise.getNotes(), exercise.isCustom());
    }
}
