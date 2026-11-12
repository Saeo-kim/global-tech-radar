package com.globaltechradar.web;

import com.globaltechradar.domain.GoalStatus;
import com.globaltechradar.domain.MarketCategory;
import com.globaltechradar.service.IntelligenceService;
import com.globaltechradar.service.PositionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/position")
public class PositionController {

    public static class SkillForm {
        @NotBlank(message = "Skill name is required") @Size(max = 80)
        private String name;
        @Size(max = 40)
        private String category = "General";
        @Min(1) @Max(5)
        private int proficiency = 3;
        @DecimalMin("0") @DecimalMax("50")
        private double yearsExperience = 1;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getProficiency() { return proficiency; }
        public void setProficiency(int proficiency) { this.proficiency = proficiency; }
        public double getYearsExperience() { return yearsExperience; }
        public void setYearsExperience(double yearsExperience) { this.yearsExperience = yearsExperience; }
    }

    public static class ProjectForm {
        @NotBlank(message = "Project name is required") @Size(max = 120)
        private String name;
        @Size(max = 80)
        private String role;
        @Size(max = 1000)
        private String description;
        @Size(max = 400)
        private String techStack;
        @Size(max = 400) @Pattern(regexp = "^$|^https?://.*", message = "URL must start with http:// or https://")
        private String url;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getTechStack() { return techStack; }
        public void setTechStack(String techStack) { this.techStack = techStack; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class GoalForm {
        @NotBlank(message = "Goal title is required") @Size(max = 160)
        private String title;
        @Size(max = 80)
        private String targetRole;
        @Size(max = 40)
        private String targetRegion = "Global";
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate targetDate;
        @Size(max = 400)
        private String requiredSkills;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getTargetRole() { return targetRole; }
        public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
        public String getTargetRegion() { return targetRegion; }
        public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }
        public LocalDate getTargetDate() { return targetDate; }
        public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
        public String getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    }

    private final PositionService position;
    private final IntelligenceService intel;

    public PositionController(PositionService position, IntelligenceService intel) {
        this.position = position;
        this.intel = intel;
    }

    @GetMapping
    public String page(Model model) {
        if (!model.containsAttribute("skillForm")) model.addAttribute("skillForm", new SkillForm());
        if (!model.containsAttribute("projectForm")) model.addAttribute("projectForm", new ProjectForm());
        if (!model.containsAttribute("goalForm")) {
            GoalForm g = new GoalForm();
            g.setTargetDate(LocalDate.now().plusMonths(6));
            model.addAttribute("goalForm", g);
        }
        model.addAttribute("skills", position.skills());
        model.addAttribute("projects", position.projects());
        model.addAttribute("goals", position.goals());
        model.addAttribute("analysis", position.analyse());
        model.addAttribute("marketSkills", intel.market(MarketCategory.SKILL_DEMAND));
        model.addAttribute("goalStatuses", GoalStatus.values());
        return "position";
    }

    @PostMapping("/skills")
    public String addSkill(@Valid @ModelAttribute("skillForm") SkillForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return flashErrors(ra, "skillForm", form, result, "skills");
        position.saveSkill(form.getName(), form.getCategory(), form.getProficiency(), form.getYearsExperience());
        ra.addFlashAttribute("toast", "Skill \"" + form.getName().trim() + "\" saved.");
        return "redirect:/position#skills";
    }

    @PostMapping("/skills/{id}/delete")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes ra) {
        position.deleteSkill(id);
        ra.addFlashAttribute("toast", "Skill removed.");
        return "redirect:/position#skills";
    }

    @PostMapping("/projects")
    public String addProject(@Valid @ModelAttribute("projectForm") ProjectForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return flashErrors(ra, "projectForm", form, result, "projects");
        position.saveProject(form.getName(), form.getRole(), form.getDescription(), form.getTechStack(), form.getUrl());
        ra.addFlashAttribute("toast", "Project saved.");
        return "redirect:/position#projects";
    }

    @PostMapping("/projects/{id}/delete")
    public String deleteProject(@PathVariable Long id, RedirectAttributes ra) {
        position.deleteProject(id);
        ra.addFlashAttribute("toast", "Project removed.");
        return "redirect:/position#projects";
    }

    @PostMapping("/goals")
    public String addGoal(@Valid @ModelAttribute("goalForm") GoalForm form, BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) return flashErrors(ra, "goalForm", form, result, "goals");
        position.saveGoal(form.getTitle(), form.getTargetRole(), form.getTargetRegion(), form.getTargetDate(), form.getRequiredSkills());
        ra.addFlashAttribute("toast", "Career goal saved.");
        return "redirect:/position#goals";
    }

    @PostMapping("/goals/{id}/status")
    public String goalStatus(@PathVariable Long id, @RequestParam GoalStatus status, RedirectAttributes ra) {
        position.updateGoalStatus(id, status);
        ra.addFlashAttribute("toast", "Goal status updated.");
        return "redirect:/position#goals";
    }

    @PostMapping("/goals/{id}/delete")
    public String deleteGoal(@PathVariable Long id, RedirectAttributes ra) {
        position.deleteGoal(id);
        ra.addFlashAttribute("toast", "Goal removed.");
        return "redirect:/position#goals";
    }

    @PostMapping("/example")
    public String example(RedirectAttributes ra) {
        position.loadExampleProfile();
        ra.addFlashAttribute("toast", "Example profile loaded. Edit or delete any entry.");
        return "redirect:/position";
    }

    private String flashErrors(RedirectAttributes ra, String name, Object form, BindingResult result, String anchor) {
        ra.addFlashAttribute("org.springframework.validation.BindingResult." + name, result);
        ra.addFlashAttribute(name, form);
        ra.addFlashAttribute("toastError", "Please fix the highlighted fields.");
        return "redirect:/position#" + anchor;
    }
}
