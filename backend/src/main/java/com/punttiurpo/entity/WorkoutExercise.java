package com.punttiurpo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout_exercise")
public class WorkoutExercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workout_id", nullable = false)
    private Workout workout;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_id", nullable = false)
    private Exercise exercise;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    @Column(length = 2000)
    private String notes;

    protected WorkoutExercise() {
    }

    public WorkoutExercise(Workout workout, Exercise exercise, int displayOrder, String notes) {
        this.workout = workout;
        this.exercise = exercise;
        update(displayOrder, notes);
    }

    public void update(int displayOrder, String notes) {
        this.displayOrder = displayOrder;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public Workout getWorkout() { return workout; }
    public Exercise getExercise() { return exercise; }
    public int getDisplayOrder() { return displayOrder; }
    public String getNotes() { return notes; }
}
