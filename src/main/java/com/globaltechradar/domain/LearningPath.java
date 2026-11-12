package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.util.List;

/** A recommended learning path for a market skill. Used by the skill-gap analysis. */
@Entity
@Table(name = "learning_path")
public class LearningPath {

    @Id
    @Column(length = 64)
    private String skillKey;   // normalised skill name (lower case)

    private String skillName;
    private String title;
    private int estimatedWeeks;

    @Column(length = 1200)
    private String steps;      // pipe separated

    protected LearningPath() { }

    public LearningPath(String skillName, String title, int estimatedWeeks, String steps) {
        this.skillKey = normalise(skillName);
        this.skillName = skillName;
        this.title = title;
        this.estimatedWeeks = estimatedWeeks;
        this.steps = steps;
    }

    public static String normalise(String skill) {
        return skill == null ? "" : skill.trim().toLowerCase();
    }

    public List<String> getStepList() { return Csv.pipes(steps); }

    public String getSkillKey() { return skillKey; }
    public String getSkillName() { return skillName; }
    public String getTitle() { return title; }
    public int getEstimatedWeeks() { return estimatedWeeks; }
    public String getSteps() { return steps; }
}
