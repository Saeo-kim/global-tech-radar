package com.globaltechradar.service;

import com.globaltechradar.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Template-based interpretation: a per-category knowledge base, keyword-triggered additions,
 * the Technology Radar's own momentum/demand values and the user's registered skills.
 * Deterministic and explainable; it makes no claims beyond the source text and the app's own data.
 */
@Service
public class RuleBasedNewsAnalyzer implements NewsAnalyzer {

    private record Knowledge(String why, List<String> industries, List<String> opportunities, List<String> projects,
                             String impact, String applications) { }

    private static final Map<NewsCategory, Knowledge> KB = new EnumMap<>(NewsCategory.class);

    static {
        KB.put(NewsCategory.AI, new Knowledge(
            "Model capability, cost and availability set the ceiling for every AI product built on top. Changes here ripple quickly into what software teams can ship and what it costs to run.",
            List.of("Software & SaaS", "Financial services", "Healthcare", "Media & marketing", "Education"),
            List.of("AI features embedded in existing products", "Evaluation, safety and governance tooling", "Domain-specific assistants built on general models"),
            List.of("A retrieval-augmented assistant over your own documents", "An evaluation harness that compares two models on your task", "A cost/latency dashboard for model API usage"),
            "Shifts what AI products can do and what they cost to operate.",
            "Developers: new model capabilities to build on. Businesses: re-check build-vs-buy and pricing assumptions."));
        KB.put(NewsCategory.AI_AGENTS, new Knowledge(
            "Agents move AI from answering questions to taking actions in software. That changes how work is automated and raises new requirements for permissions, auditing and testing.",
            List.of("Software development", "Customer operations", "IT operations", "Back-office finance", "E-commerce"),
            List.of("Agent-ready APIs and tool integrations", "Observability and approval workflows for agents", "Vertical agents for regulated processes"),
            List.of("An agent that triages GitHub issues and drafts fixes for review", "A tool server exposing an internal API to agents with scoped permissions", "A human-in-the-loop approval queue for agent actions"),
            "Automation of multi-step work moves closer to production use.",
            "Developers: expose clean, permissioned tools. Businesses: pilot narrow, auditable agent workflows."));
        KB.put(NewsCategory.ROBOTICS, new Knowledge(
            "Robotics progress turns AI into physical labour. It matters most where labour is scarce or tasks are repetitive and hazardous.",
            List.of("Manufacturing", "Logistics & warehousing", "Healthcare", "Agriculture", "Construction"),
            List.of("Fleet management and orchestration software", "Simulation and synthetic training data", "Robot-as-a-service business models"),
            List.of("A ROS 2 simulation of a pick-and-place task", "A fleet dashboard that schedules mobile robots", "A vision model that detects grasp points"),
            "Brings automation to physical tasks in factories and warehouses.",
            "Developers: simulation, perception and fleet software. Businesses: evaluate labour-intensive processes."));
        KB.put(NewsCategory.SEMICONDUCTOR, new Knowledge(
            "Chips are the physical bottleneck for AI and cloud capacity. Supply, performance and export rules decide who can build and deploy at scale.",
            List.of("Cloud & data centers", "Consumer electronics", "Automotive", "Telecommunications", "Defense"),
            List.of("Inference optimisation for new hardware", "Hardware-aware model serving", "Supply-chain analytics"),
            List.of("Benchmark one model on two accelerator types", "Quantise a model and measure the quality/latency trade-off", "A dashboard tracking GPU utilisation and cost"),
            "Affects AI compute supply, cost and performance.",
            "Developers: optimise for the hardware you run on. Businesses: plan compute capacity and vendor risk."));
        KB.put(NewsCategory.SOFTWARE_ENGINEERING, new Knowledge(
            "Languages, frameworks and tooling decide developer productivity and maintenance cost. Shifts here change hiring profiles and engineering practices.",
            List.of("Software & SaaS", "IT services", "Financial services", "Public sector digital services"),
            List.of("Developer tooling and productivity products", "Migration and modernisation services", "Training and enablement"),
            List.of("Migrate a small service to the new version and document the changes", "A CI check that enforces the new practice", "A starter template using the new tool"),
            "Changes developer tooling, productivity and maintenance costs.",
            "Developers: evaluate the tool on a small project. Businesses: assess migration effort and benefits."));
        KB.put(NewsCategory.CLOUD_DEVOPS, new Knowledge(
            "Cloud and delivery platforms determine how fast and how cheaply software reaches production. Changes affect architecture, cost and reliability.",
            List.of("Software & SaaS", "E-commerce", "Media streaming", "Financial services", "Public sector"),
            List.of("Platform engineering and internal developer platforms", "Cloud cost optimisation (FinOps)", "Managed migrations"),
            List.of("Deploy a service with GitOps on a local Kubernetes cluster", "A cost report that tags cloud spend by team", "A golden-path template for new services"),
            "Influences deployment speed, cloud cost and reliability.",
            "Developers: adopt new platform capabilities. Businesses: review architecture and cost impact."));
        KB.put(NewsCategory.CYBERSECURITY, new Knowledge(
            "Security developments change risk for every organisation that runs software. Advisories and new attack techniques often require immediate action.",
            List.of("All industries", "Financial services", "Healthcare", "Critical infrastructure", "Government"),
            List.of("Managed detection and response", "Vulnerability and patch management tools", "Security automation for small teams"),
            List.of("A dashboard that matches your dependencies against new advisories", "An SBOM generator in your CI pipeline", "A phishing-resistant login demo with passkeys"),
            "Changes risk exposure; may require patching or configuration changes.",
            "Developers: check affected dependencies and harden defaults. Businesses: review exposure and response plans."));
        KB.put(NewsCategory.DATA_ENGINEERING, new Knowledge(
            "Data platforms feed analytics and AI. Changes affect data quality, cost and how quickly teams can build AI features on their own data.",
            List.of("Retail & e-commerce", "Financial services", "Manufacturing", "Healthcare", "Media"),
            List.of("Data platform modernisation", "Data quality and governance tooling", "AI-ready data products"),
            List.of("A streaming pipeline into a lakehouse table", "A data-quality check suite for a public dataset", "Semantic search over a product catalogue"),
            "Affects how quickly organisations can turn data into analytics and AI.",
            "Developers: pipelines, table formats and quality checks. Businesses: data readiness for AI."));
        KB.put(NewsCategory.SPACE, new Knowledge(
            "Space technology is becoming commercial infrastructure: connectivity, Earth observation and navigation increasingly depend on it.",
            List.of("Telecommunications", "Agriculture", "Insurance", "Logistics", "Defense", "Climate & energy"),
            List.of("Earth-observation analytics", "Satellite connectivity applications", "Ground-segment and mission software"),
            List.of("Analyse public satellite imagery for land-use change", "A dashboard of upcoming launches and missions from public APIs", "An orbit visualiser using public TLE data"),
            "Expands commercial uses of satellite data and connectivity.",
            "Developers: geospatial and imagery analysis. Businesses: Earth-observation and connectivity use cases."));
        KB.put(NewsCategory.EMERGING, new Knowledge(
            "Emerging technologies are early, so the main value is in tracking milestones and building skills before markets form.",
            List.of("Research & academia", "Pharma & materials", "Finance", "Telecommunications"),
            List.of("Proof-of-concept consulting", "Education and training", "Early tooling for new platforms"),
            List.of("Run a tutorial on a cloud-accessible quantum simulator", "A literature tracker for a niche technology", "A prototype demo for an emerging interface"),
            "Early signal; long-term impact is uncertain.",
            "Developers: small experiments and learning. Businesses: watch milestones; avoid premature bets."));
    }

    /** Keyword-triggered additions on top of the category template: {pattern, opportunity, project}. */
    private static final List<Object[]> TRIGGERS = List.of(
        new Object[]{p("open[- ]source|open[- ]weight|github"), "Build on the openly released code or models", "Fork the released project and adapt it to a local use case"},
        new Object[]{p("api|sdk|developer preview|public preview"), "Integrate the new API into existing products early", "A small integration demo using the new API or SDK"},
        new Object[]{p("vulnerab|cve|exploit|zero-day|advisory"), "Patch and exposure-management services", "Scan a test environment for the affected components"},
        new Object[]{p("funding|raises|investment|acquire|acquisition"), "Partnerships with newly funded or merged players", null},
        new Object[]{p("regulat|law|policy|act\\b|government"), "Compliance tooling and advisory for the new rules", "A checklist app mapping the new rule to engineering controls"},
        new Object[]{p("benchmark|record|faster|efficien"), "Performance-sensitive products that benefit from the gains", "Reproduce the reported benchmark on your own workload"});

    private static Pattern p(String regex) { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE); }

    private final IntelligenceService intel;
    private final PositionService position;

    public RuleBasedNewsAnalyzer(IntelligenceService intel, PositionService position) {
        this.intel = intel;
        this.position = position;
    }

    @Override
    public String method() {
        return "Rule-based interpretation (category templates + radar and profile data)";
    }

    @Override
    public Brief brief(NewsItem item) {
        Knowledge k = KB.get(item.getCategory() == null ? NewsCategory.EMERGING : item.getCategory());
        return new Brief(k.impact(), k.applications());
    }

    @Override
    public Analysis analyse(NewsItem item) {
        NewsCategory cat = item.getCategory() == null ? NewsCategory.EMERGING : item.getCategory();
        Knowledge k = KB.get(cat);
        String text = item.getTitle() + " " + (item.getSummary() == null ? "" : item.getSummary());

        LinkedHashSet<String> opportunities = new LinkedHashSet<>(k.opportunities());
        LinkedHashSet<String> projects = new LinkedHashSet<>(k.projects());
        for (Object[] t : TRIGGERS) {
            if (((Pattern) t[0]).matcher(text).find()) {
                opportunities.add((String) t[1]);
                if (t[2] != null) projects.add((String) t[2]);
            }
        }

        Map<String, Company> tracked = new HashMap<>();
        intel.companies().forEach(c -> tracked.put(c.getName().toLowerCase(Locale.ROOT), c));
        List<Company> trackedCompanies = item.getCompanyList().stream()
            .map(n -> tracked.get(n.toLowerCase(Locale.ROOT))).filter(Objects::nonNull).toList();

        List<Technology> related = cat.getRadarDomains().stream()
            .flatMap(d -> intel.technologiesInDomain(d).stream()).distinct()
            .sorted(Comparator.comparingInt(Technology::getMomentum).reversed()).toList();

        String why = k.why();
        if (!trackedCompanies.isEmpty()) {
            why += " It involves " + names(trackedCompanies) + ", tracked in Company Intelligence.";
        }

        List<String> reasons = new ArrayList<>();
        String verdict = verdict(related, reasons);

        return new Analysis(method(),
            "This section is an automated interpretation, not a fact from the source. It combines category templates, "
                + "keywords in the source text, Technology Radar values (sample data) and your own skill profile.",
            why, k.industries(), item.getCompanyList(), trackedCompanies,
            List.copyOf(opportunities).subList(0, Math.min(5, opportunities.size())), verdict, reasons,
            List.copyOf(projects).subList(0, Math.min(5, projects.size())), related.stream().limit(4).toList());
    }

    private String verdict(List<Technology> related, List<String> reasons) {
        if (related.isEmpty()) {
            reasons.add("No matching technology on the radar yet.");
            return "Monitor";
        }
        double momentum = related.stream().mapToInt(Technology::getMomentum).average().orElse(0);
        double demand = related.stream().mapToInt(Technology::getMarketDemand).average().orElse(0);
        Technology top = related.get(0);
        reasons.add("Related radar technology: " + top.getName() + " (" + top.getRing().getLabel() + ", momentum "
            + top.getMomentum() + "/100).");
        reasons.add(String.format("Average momentum %.0f and market demand %.0f across %d related radar entries (sample data).",
            momentum, demand, related.size()));

        Set<String> relatedSkills = new LinkedHashSet<>();
        related.forEach(t -> t.getRelatedSkillList().forEach(s -> relatedSkills.add(PositionService.key(s))));
        Optional<UserSkill> have = position.skills().stream()
            .filter(s -> relatedSkills.contains(PositionService.key(s.getName())))
            .max(Comparator.comparingInt(UserSkill::getProficiency));

        if (have.isPresent() && have.get().getProficiency() >= 3) {
            reasons.add("You already list " + have.get().getName() + " at level " + have.get().getProficiency() + "/5 in My Technology Position.");
            return "Apply it now";
        }
        if (have.isPresent()) reasons.add("You list " + have.get().getName() + " at level " + have.get().getProficiency() + "/5; deepening it is a short step.");
        else reasons.add("None of the related skills (" + String.join(", ", relatedSkills.stream().limit(4).toList()) + ") are in your profile yet.");
        if (momentum >= 75 && demand >= 65) return "Worth learning now";
        if (momentum >= 55) return "Explore with a small prototype";
        return "Monitor";
    }

    private static String names(List<Company> cs) {
        return String.join(", ", cs.stream().map(Company::getName).toList());
    }
}
