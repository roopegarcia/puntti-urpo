package com.punttiurpo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercise")
public class Exercise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(name = "primary_muscle_group", length = 50)
    private String primaryMuscleGroup;

    @Column(length = 50)
    private String equipment;

    @Column(length = 2000)
    private String notes;

    @Column(nullable = false, updatable = false)
    private boolean custom = true;

    protected Exercise() {
        // JPA needs a no-argument constructor to reconstruct persisted entities.
    }

    public Exercise(String name, String category, String primaryMuscleGroup,
                    String equipment, String notes) {
        update(name, category, primaryMuscleGroup, equipment, notes);
    }

    public void update(String name, String category, String primaryMuscleGroup,
                       String equipment, String notes) {
        this.name = name;
        this.category = category;
        this.primaryMuscleGroup = primaryMuscleGroup;
        this.equipment = equipment;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getPrimaryMuscleGroup() { return primaryMuscleGroup; }
    public String getEquipment() { return equipment; }
    public String getNotes() { return notes; }
    public boolean isCustom() { return custom; }
}
