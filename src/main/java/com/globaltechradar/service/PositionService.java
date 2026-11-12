package com.globaltechradar.service;

import com.globaltechradar.domain.*;
import com.globaltechradar.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * "My Technology Position": the user's skills, projects and goals, compared with market demand.
 */
@Service
public class PositionService {

    /** Common shorthand mapped to the market skill names used in the dataset. */
    private static final Map<String, String> ALIASES = Map.ofEntries(
        Map.entry("k8s", "kubernetes"),
        Map.entry("ts", "typescript"),
        Map.entry("js", "typescript"),
        Map.entry("javascript", "typescript"),
        Map.entry("spark", "data engineering (spark)"),
        Map.entry("apache spark", "data engineering (spark)"),
        Map.entry("llm", "llm application development"),
        Map.entry("llms", "llm application development"),
        Map.entry("rag", "llm application development"),
        Map.entry("agents", "ai agents"),
        Map.entry("agentic ai", "ai agents"),
        Map.entry("cuda", "cuda / gpu programming"),
        Map.entry("spring", "spring boot"),
        Map.entry("springboot", "spring boot"),
        Map.entry("golang", "go"),
        Map.entry("security", "cybersecurity"),
        Map.entry("amazon web services", "aws"),
        Map.entry("reactjs", "react"),
        Map.entry("react.js", "react"),
        Map.entry("mysql", "sql"),
        Map.entry("postgresql", "sql")
    );

    private final UserSkillRepository skills;
    private final UserProjectRepository projects;
    private final CareerGoalRepository goals;
    private final LearningPathRepository learningPaths;
    private final IntelligenceService intelligence;

    public PositionService(UserSkillRepository skills, UserProjectRepository projects, CareerGoalRepository goals,
                           LearningPathRepository learningPaths, IntelligenceService intelligence) {
        this.skills = skills;
        this.projects = projects;
        this.goals = goals;
        this.learningPaths = learningPaths;
        this.intelligence = intelligence;
    }

    static String key(String skill) {
        String k = LearningPath.normalise(skill);
        return ALIASES.getOrDefault(k, k);
    }

    // ---------------------------------------------------------------- CRUD

    public List<UserSkill> skills() { return skills.findAllByOrderByProficiencyDescNameAsc(); }

    public List<UserProject> projects() { return projects.findAllByOrderByCreatedAtDesc(); }

    public List<CareerGoal> goals() { return goals.findAllByOrderByTargetDateAsc(); }

    /** Adds a skill, or updates it when a skill with the same name already exists. */
    @Transactional
    public UserSkill saveSkill(String name, String category, int proficiency, double years) {
        int level = Math.max(1, Math.min(5, proficiency));
        double y = Math.max(0, Math.min(50, years));
        Optional<UserSkill> existing = skills.findFirstByNameIgnoreCase(name.trim());
        if (existing.isPresent()) {
            existing.get().update(category, level, y);
            return existing.get();
        }
        return skills.save(new UserSkill(name.trim(), category, level, y));
    }

    @Transactional
    public void deleteSkill(Long id) { skills.deleteById(id); }

    @Transactional
    public UserProject saveProject(String name, String role, String description, String techStack, String url) {
        return projects.save(new UserProject(name.trim(), role, description, techStack, url));
    }

    @Transactional
    public void deleteProject(Long id) { projects.deleteById(id); }

    @Transactional
    public CareerGoal saveGoal(String title, String targetRole, String targetRegion, LocalDate targetDate, String requiredSkills) {
        return goals.save(new CareerGoal(title.trim(), targetRole, targetRegion, targetDate, requiredSkills));
    }

    @Transactional
    public void updateGoalStatus(Long id, GoalStatus status) {
        goals.findById(id).ifPresent(g -> g.setStatus(status));
    }

    @Transactional
    public void deleteGoal(Long id) { goals.deleteById(id); }

    /** Loads an example profile so a first-time user can see the analysis working. */
    @Transactional
    public void loadExampleProfile() {
        saveSkill("Java", "Language", 4, 3);
        saveSkill("Spring Boot", "Backend", 4, 2.5);
        saveSkill("SQL", "Data", 3, 3);
        saveSkill("React", "Frontend", 2, 1);
        saveSkill("Python", "Language", 2, 1);
        saveSkill("AWS", "Cloud", 2, 1);
        if (projects.count() == 0) {
            saveProject("Freelance project manager", "Full-stack developer",
                "Spring Boot + MySQL web app for tracking freelance clients, invoices and deadlines.",
                "Java,Spring Boot,MySQL,Thymeleaf", "");
        }
        if (goals.count() == 0) {
            saveGoal("Become an AI-enabled backend engineer", "Backend / AI Engineer", "Global",
                LocalDate.now().plusMonths(9), "LLM application development,AI agents,Kubernetes,Python");
        }
    }

    // ---------------------------------------------------------------- analysis

    public record SkillComparison(String skill, String category, int marketDemand, double marketGrowth,
                                  int userLevel, int userProficiency, int gap, boolean goalSkill,
                                  double priority, String status) { }

    public record Recommendation(String skill, String reason, int gap, double priority, LearningPath path) { }

    public record Analysis(int alignmentScore, int skillsTracked, int marketSkillsCovered, int strongSkills,
                           List<SkillComparison> comparisons, List<Recommendation> recommendations,
                           List<UserSkill> offMarketSkills, List<String> goalSkillsMissing) { }

    @Transactional(readOnly = true)
    public Analysis analyse() {
        List<UserSkill> mine = skills();
        Map<String, UserSkill> byKey = new HashMap<>();
        mine.forEach(s -> byKey.putIfAbsent(key(s.getName()), s));

        Set<String> goalSkills = new LinkedHashSet<>();
        goals().stream().filter(g -> g.getStatus() != GoalStatus.ACHIEVED)
            .forEach(g -> g.getRequiredSkillList().forEach(s -> goalSkills.add(key(s))));

        List<MarketSignal> market = intelligence.market(MarketCategory.SKILL_DEMAND);
        Set<String> marketKeys = new HashSet<>();
        List<SkillComparison> comparisons = new ArrayList<>();
        double demandSum = 0;
        double coveredSum = 0;
        for (MarketSignal m : market) {
            String k = key(m.getTitle());
            marketKeys.add(k);
            int demand = (int) Math.round(m.getMetricValue());
            double growth = m.getSecondaryValue() == null ? 0 : m.getSecondaryValue();
            UserSkill s = byKey.get(k);
            int proficiency = s == null ? 0 : s.getProficiency();
            int level = proficiency * 20;
            int gap = Math.max(0, demand - level);
            boolean isGoal = goalSkills.contains(k);
            double priority = gap * (demand / 100.0) + Math.min(growth, 60) * 0.4 + (isGoal ? 20 : 0);
            String status = level >= demand ? "Strong" : level >= demand - 25 ? "Developing" : s == null ? "Missing" : "Gap";
            comparisons.add(new SkillComparison(m.getTitle(), m.getTags(), demand, growth, level, proficiency, gap,
                isGoal, Math.round(priority * 10) / 10.0, status));
            demandSum += demand;
            coveredSum += Math.min(level, demand);
        }
        comparisons.sort(Comparator.comparingDouble(SkillComparison::priority).reversed());

        Map<String, LearningPath> paths = new HashMap<>();
        learningPaths.findAll().forEach(p -> paths.put(p.getSkillKey(), p));

        List<Recommendation> recs = comparisons.stream()
            .filter(c -> c.gap() > 0)
            .limit(5)
            .map(c -> new Recommendation(c.skill(), reason(c), c.gap(), c.priority(), paths.get(key(c.skill()))))
            .toList();

        List<UserSkill> offMarket = mine.stream().filter(s -> !marketKeys.contains(key(s.getName()))).toList();
        List<String> goalMissing = goals().stream().filter(g -> g.getStatus() != GoalStatus.ACHIEVED)
            .flatMap(g -> g.getRequiredSkillList().stream())
            .filter(s -> !byKey.containsKey(key(s)))
            .distinct().toList();

        int alignment = demandSum == 0 ? 0 : (int) Math.round(coveredSum / demandSum * 100);
        int covered = (int) comparisons.stream().filter(c -> c.userProficiency() > 0).count();
        int strong = (int) comparisons.stream().filter(c -> "Strong".equals(c.status())).count();
        return new Analysis(alignment, mine.size(), covered, strong, comparisons, recs, offMarket, goalMissing);
    }

    private static String reason(SkillComparison c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Market demand ").append(c.marketDemand()).append("/100");
        if (c.marketGrowth() >= 10) sb.append(", growing fast (+").append(Math.round(c.marketGrowth())).append("% YoY)");
        if (c.userProficiency() == 0) sb.append(". Not in your profile yet");
        else sb.append(". You are at level ").append(c.userProficiency()).append("/5");
        if (c.goalSkill()) sb.append(". Required by one of your career goals");
        return sb.append('.').toString();
    }
}
