package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "career_goal")
public class CareerGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String targetRole;
    private String targetRegion;
    private LocalDate targetDate;

    /** Skills this goal requires, comma separated. Feeds the gap analysis. */
    @Column(length = 400)
    private String requiredSkills;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GoalStatus status = GoalStatus.PLANNED;

    protected CareerGoal() { }

    public CareerGoal(String title, String targetRole, String targetRegion, LocalDate targetDate, String requiredSkills) {
        this.title = title;
        this.targetRole = targetRole;
        this.targetRegion = targetRegion;
        this.targetDate = targetDate;
        this.requiredSkills = requiredSkills;
    }

    public void setStatus(GoalStatus status) { this.status = status; }

    public List<String> getRequiredSkillList() { return Csv.split(requiredSkills); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getTargetRole() { return targetRole; }
    public String getTargetRegion() { return targetRegion; }
    public LocalDate getTargetDate() { return targetDate; }
    public String getRequiredSkills() { return requiredSkills; }
    public GoalStatus getStatus() { return status; }
}
