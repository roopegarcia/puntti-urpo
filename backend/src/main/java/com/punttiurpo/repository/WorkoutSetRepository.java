package com.punttiurpo.repository;

import java.util.List;
import com.punttiurpo.entity.WorkoutSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, Long> {
    java.util.Optional<WorkoutSet> findByIdAndWorkoutExerciseWorkoutOwnerKey(long id, String ownerKey);
    List<WorkoutSet> findByWorkoutExerciseIdOrderBySetOrderAscIdAsc(long workoutExerciseId);

    List<WorkoutSet> findByWorkoutExerciseWorkoutIdOrderBySetOrderAscIdAsc(long workoutId);

    @Query("select coalesce(max(s.setOrder), -1) from WorkoutSet s where s.workoutExercise.id = :exerciseId")
    int maxOrder(long exerciseId);
}
