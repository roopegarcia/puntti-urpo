package com.punttiurpo.repository;

import java.util.List;
import com.punttiurpo.entity.WorkoutExercise;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {
    @EntityGraph(attributePaths = "exercise")
    List<WorkoutExercise> findByWorkoutIdOrderByDisplayOrderAscIdAsc(long workoutId);

    @Query("select coalesce(max(e.displayOrder), -1) from WorkoutExercise e where e.workout.id = :workoutId")
    int maxOrder(long workoutId);
}
