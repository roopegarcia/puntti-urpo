package com.punttiurpo.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_set")
public class WorkoutSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_exercise_id", nullable = false)
    private WorkoutExercise workoutExercise;
    @Column(name = "set_order", nullable = false)
    private int setOrder;
    @Column(name = "weight_kg", precision = 6, scale = 2)
    private BigDecimal weightKg;
    private Integer repetitions;
    @Column(precision = 3, scale = 1)
    private BigDecimal rpe;
    @Enumerated(EnumType.STRING)
    @Column(name = "set_type", nullable = false, length = 10)
    private WorkoutSetType setType;
    @Column(nullable = false)
    private boolean completed;

    protected WorkoutSet() {
    }

    public WorkoutSet(WorkoutExercise workoutExercise, int setOrder, BigDecimal weightKg,
                      Integer repetitions, BigDecimal rpe, WorkoutSetType setType, boolean completed) {
        this.workoutExercise = workoutExercise;
        update(setOrder, weightKg, repetitions, rpe, setType, completed);
    }

    public void update(int setOrder, BigDecimal weightKg, Integer repetitions, BigDecimal rpe,
                       WorkoutSetType setType, boolean completed) {
        this.setOrder = setOrder;
        this.weightKg = weightKg;
        this.repetitions = repetitions;
        this.rpe = rpe;
        this.setType = setType;
        this.completed = completed;
    }

    public Long getId() { return id; }
    public WorkoutExercise getWorkoutExercise() { return workoutExercise; }
    public int getSetOrder() { return setOrder; }
    public BigDecimal getWeightKg() { return weightKg; }
    public Integer getRepetitions() { return repetitions; }
    public BigDecimal getRpe() { return rpe; }
    public WorkoutSetType getSetType() { return setType; }
    public boolean isCompleted() { return completed; }
}
