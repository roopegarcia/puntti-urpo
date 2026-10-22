package com.punttiurpo.mapper;

import java.util.List;
import com.punttiurpo.dto.WorkoutExerciseResponse;
import com.punttiurpo.dto.WorkoutResponse;
import com.punttiurpo.dto.WorkoutSetResponse;
import com.punttiurpo.dto.WorkoutSummaryResponse;
import com.punttiurpo.entity.Workout;
import com.punttiurpo.entity.WorkoutExercise;
import com.punttiurpo.entity.WorkoutSet;

public final class WorkoutMapper {
    private WorkoutMapper() {
    }

    public static WorkoutSummaryResponse summary(Workout workout) {
        return new WorkoutSummaryResponse(workout.getId(), workout.getDate(), workout.getTitle(),
                workout.getNotes(), workout.getStartedAt(), workout.getEndedAt());
    }

    public static WorkoutSetResponse set(WorkoutSet set) {
        return new WorkoutSetResponse(set.getId(), set.getSetOrder(), set.getWeightKg(),
                set.getRepetitions(), set.getRpe(), set.getSetType(), set.isCompleted());
    }

    public static WorkoutExerciseResponse exercise(WorkoutExercise exercise, List<WorkoutSetResponse> sets) {
        return new WorkoutExerciseResponse(exercise.getId(), ExerciseMapper.toResponse(exercise.getExercise()),
                exercise.getDisplayOrder(), exercise.getNotes(), sets);
    }

    public static WorkoutResponse workout(Workout workout, List<WorkoutExerciseResponse> exercises) {
        return new WorkoutResponse(workout.getId(), workout.getDate(), workout.getTitle(), workout.getNotes(),
                workout.getStartedAt(), workout.getEndedAt(), exercises);
    }
}
