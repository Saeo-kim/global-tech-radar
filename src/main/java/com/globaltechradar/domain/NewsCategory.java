package com.globaltechradar.domain;

import java.util.List;

/**
 * Categories of the "Latest Global Tech Updates" section.
 * {@code radarDomains} links a category to the existing Technology Radar domains;
 * {@code keywords} drive automatic classification of collected articles.
 */
public enum NewsCategory {
    AI("Artificial Intelligence", List.of("AI"),
        List.of("artificial intelligence", "ai", "llm", "llms", "language model", "machine learning", "neural network",
            "generative", "foundation model", "gemini", "gpt", "multimodal", "reasoning model", "deep learning")),
    AI_AGENTS("AI Agents", List.of("AI Agents"),
        List.of("agent", "agents", "agentic", "copilot", "ai assistant", "mcp", "tool use", "computer use", "autonomous coding")),
    ROBOTICS("Robotics & Humanoid Robots", List.of("Robotics"),
        List.of("robot", "robots", "robotics", "humanoid", "manipulation", "drone", "drones", "autonomous vehicle",
            "self-driving", "exoskeleton", "cobot")),
    SEMICONDUCTOR("Semiconductor", List.of("Semiconductor"),
        List.of("chip", "chips", "semiconductor", "semiconductors", "gpu", "gpus", "wafer", "foundry", "lithography",
            "hbm", "transistor", "packaging", "chiplet", "chiplets", "fab", "asic", "tpu", "silicon")),
    SOFTWARE_ENGINEERING("Software Engineering", List.of("Software Engineering"),
        List.of("programming", "developer", "developers", "open source", "open-source", "rust", "typescript", "java",
            "python", "compiler", "framework", "github", "code review", "software engineering", "api", "sdk")),
    CLOUD_DEVOPS("Cloud & DevOps", List.of("Cloud", "DevOps"),
        List.of("cloud", "kubernetes", "devops", "serverless", "container", "containers", "aws", "azure", "ci/cd",
            "platform engineering", "observability", "data center", "datacenter", "infrastructure")),
    CYBERSECURITY("Cybersecurity", List.of("Cybersecurity"),
        List.of("security", "vulnerability", "vulnerabilities", "cve", "ransomware", "malware", "exploit", "exploited",
            "phishing", "zero-day", "encryption", "cyber", "cybersecurity", "patch", "advisory", "threat", "post-quantum")),
    DATA_ENGINEERING("Data Engineering", List.of("Data Engineering"),
        List.of("data pipeline", "database", "databases", "lakehouse", "spark", "sql", "data engineering", "warehouse",
            "streaming", "vector database", "etl", "analytics", "data platform", "iceberg", "postgres")),
    SPACE("Space Technology", List.of("Emerging Technologies"),
        List.of("space", "satellite", "satellites", "nasa", "esa", "rocket", "orbit", "orbital", "lunar", "moon",
            "mars", "spacecraft", "astronaut", "telescope", "launch vehicle", "artemis")),
    EMERGING("Emerging Technologies", List.of("Emerging Technologies"),
        List.of("quantum", "qubit", "qubits", "fusion", "6g", "spatial computing", "augmented reality", "virtual reality",
            "brain-computer", "neuromorphic", "biotech", "battery", "photonic", "photonics"));

    private final String label;
    private final List<String> radarDomains;
    private final List<String> keywords;

    NewsCategory(String label, List<String> radarDomains, List<String> keywords) {
        this.label = label;
        this.radarDomains = radarDomains;
        this.keywords = keywords;
    }

    public String getLabel() { return label; }
    public List<String> getRadarDomains() { return radarDomains; }
    public List<String> getKeywords() { return keywords; }
}
