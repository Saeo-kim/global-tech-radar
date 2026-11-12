package com.globaltechradar.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "user_project")
public class UserProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String role;

    @Column(length = 1000)
    private String description;

    @Column(length = 400)
    private String techStack;   // comma separated

    @Column(length = 400)
    private String url;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserProject() { }

    public UserProject(String name, String role, String description, String techStack, String url) {
        this.name = name;
        this.role = role;
        this.description = description;
        this.techStack = techStack;
        this.url = url;
    }

    public List<String> getTechStackList() { return Csv.split(techStack); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getDescription() { return description; }
    public String getTechStack() { return techStack; }
    public String getUrl() { return url; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
