package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_skill")
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category;
    /** 1 = beginner ... 5 = expert. */
    private int proficiency;
    private double yearsExperience;
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserSkill() { }

    public UserSkill(String name, String category, int proficiency, double yearsExperience) {
        this.name = name;
        this.category = category;
        this.proficiency = proficiency;
        this.yearsExperience = yearsExperience;
    }

    public void update(String category, int proficiency, double yearsExperience) {
        this.category = category;
        this.proficiency = proficiency;
        this.yearsExperience = yearsExperience;
    }

    public String getProficiencyLabel() {
        return switch (proficiency) {
            case 1 -> "Beginner";
            case 2 -> "Elementary";
            case 3 -> "Intermediate";
            case 4 -> "Advanced";
            default -> "Expert";
        };
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getProficiency() { return proficiency; }
    public double getYearsExperience() { return yearsExperience; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
