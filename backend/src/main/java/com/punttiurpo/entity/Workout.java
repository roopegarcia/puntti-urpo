package com.punttiurpo.entity;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "workout")
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private LocalDate date;
    @Column(length = 100)
    private String title;
    @Column(length = 2000)
    private String notes;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "ended_at")
    private Instant endedAt;

    protected Workout() {
    }

    public Workout(LocalDate date, String title, String notes, Instant startedAt, Instant endedAt) {
        update(date, title, notes, startedAt, endedAt);
    }

    public void update(LocalDate date, String title, String notes, Instant startedAt, Instant endedAt) {
        this.date = date;
        this.title = title;
        this.notes = notes;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public String getTitle() { return title; }
    public String getNotes() { return notes; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
}
