package com.globaltechradar.provider;

import com.globaltechradar.domain.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.globaltechradar.domain.RadarQuadrant.*;
import static com.globaltechradar.domain.RadarRing.*;

/**
 * Bundled, clearly-labelled ILLUSTRATIVE dataset.
 * <p>
 * Every number here is a hand-authored sample value chosen to make the dashboard meaningful.
 * None of it is measured, scraped or live, and none of it should be quoted as a statistic.
 * Each record carries {@link DataStatus#SAMPLE} provenance and names the real-world source a
 * future live integration would use ("planned source").
 */
@Component
public class SampleIntelligenceDataProvider implements IntelligenceDataProvider {

    public static final String ID = "sample";
    public static final String SOURCE = "GTR Illustrative Sample Dataset v1";
    /** Date the sample dataset was compiled. Shown as the "published" date of sample records. */
    public static final LocalDate COMPILED_ON = LocalDate.of(2026, 11, 1);

    private static DataProvenance sample(String plannedSource) {
        return DataProvenance.sample(SOURCE, COMPILED_ON, plannedSource);
    }

    private static DataProvenance sample(String plannedSource, LocalDate date) {
        return DataProvenance.sample(SOURCE, date, plannedSource);
    }

    @Override public String id() { return ID; }

    @Override public String displayName() { return "Bundled sample dataset (illustrative)"; }

    @Override public DataStatus dataStatus() { return DataStatus.SAMPLE; }

    // ------------------------------------------------------------------ countries

    @Override
    public List<Country> countries() {
        String planned = "World Bank Open Data API, OECD MSTI, national statistics offices";
        return List.of(
            new Country("US", "United States", "North America", "🇺🇸", 38.0, -97.0,
                94, 96, 92, 88, 97, 90, 88, 4400,
                "Frontier AI labs, Venture capital depth, Cloud hyperscalers, Chip design",
                "ai,ai-agents,cloud,semiconductor,cybersecurity",
                "Deepest combined pool of frontier-model labs, hyperscale cloud and venture funding. Policy focus is shifting toward export controls, domestic chip manufacturing incentives and AI safety evaluation.",
                sample(planned)),
            new Country("CN", "China", "East Asia", "🇨🇳", 35.0, 103.0,
                86, 89, 85, 84, 82, 88, 48, 7000,
                "Manufacturing scale, Open-weight models, EV and battery supply chain, Robotics deployment",
                "ai,robotics,semiconductor,cloud",
                "Very large engineering workforce and fast industrial deployment of AI and robotics. Strong push for self-sufficiency in semiconductors and open-weight model ecosystems.",
                sample(planned)),
            new Country("JP", "Japan", "East Asia", "🇯🇵", 36.0, 138.0,
                74, 70, 72, 86, 58, 85, 55, 1100,
                "Industrial robotics, Materials and equipment, Automotive software, Precision manufacturing",
                "robotics,semiconductor,ai",
                "World-class robotics and semiconductor materials base. Digital-transformation programs and a tight labour market create steady demand for automation and cloud skills.",
                sample(planned)),
            new Country("KR", "South Korea", "East Asia", "🇰🇷", 36.5, 127.8,
                80, 78, 76, 93, 70, 94, 60, 500,
                "Memory semiconductors, 5G and digital infrastructure, Consumer electronics, Gaming",
                "semiconductor,ai,cloud,robotics",
                "Exceptionally high R&D intensity and digital infrastructure. Memory chips for AI accelerators are a strategic strength; sovereign-AI and on-device AI initiatives are growing.",
                sample(planned)),
            new Country("IN", "India", "South Asia", "🇮🇳", 21.0, 78.0,
                72, 70, 88, 48, 78, 64, 82, 5800,
                "Developer talent scale, Global capability centres, Digital public infrastructure, IT services",
                "software-engineering,cloud,ai,data-engineering",
                "One of the fastest-growing developer populations, anchored by IT services and global capability centres. Digital public infrastructure is a model other countries study.",
                sample(planned)),
            new Country("DE", "Germany", "Europe", "🇩🇪", 51.0, 10.0,
                76, 72, 74, 82, 68, 78, 74, 950,
                "Industrial automation, Automotive engineering, Mittelstand B2B software, Applied research institutes",
                "robotics,cybersecurity,cloud,ai",
                "Strong industrial and applied-research base. Industrial AI, sovereign cloud and cybersecurity compliance are the main growth vectors for technology talent.",
                sample(planned)),
            new Country("GB", "United Kingdom", "Europe", "🇬🇧", 54.0, -2.0,
                81, 82, 80, 74, 84, 82, 86, 900,
                "AI research, Fintech, AI safety evaluation, University spin-outs",
                "ai,cybersecurity,blockchain,ai-agents",
                "Leading European hub for AI research and fintech, with an active AI safety and evaluation ecosystem. Strong remote-work culture in software roles.",
                sample(planned)),
            new Country("FR", "France", "Europe", "🇫🇷", 46.0, 2.0,
                74, 76, 72, 72, 73, 79, 72, 650,
                "Open-weight AI labs, Mathematics talent, Aerospace and defence, Public AI investment",
                "ai,cloud,cybersecurity,emerging-tech",
                "Growing AI-lab scene built on deep mathematics talent and public investment programs. Sovereign cloud and AI regulation readiness are recurring themes.",
                sample(planned)),
            new Country("SG", "Singapore", "Southeast Asia", "🇸🇬", 1.35, 103.82,
                83, 84, 74, 76, 80, 96, 84, 180,
                "Regional HQ hub, Digital infrastructure, Fintech regulation, Data-centre capacity",
                "ai,cloud,cybersecurity,blockchain",
                "Compact but highly connected hub for Asia-Pacific regional headquarters, fintech and data centres. National AI strategy emphasises applied AI and governance.",
                sample(planned))
        );
    }

    // ------------------------------------------------------------------ technologies

    @Override
    public List<Technology> technologies() {
        String planned = "GitHub / package-registry trends, job-posting APIs, analyst radars";
        List<Technology> list = new ArrayList<>();
        // AI
        list.add(tech("foundation-models", "Foundation models", "AI", AI_DATA, ADOPT, 82, 92, 94, 31,
            "General-purpose large models used through APIs or self-hosted open weights as the base for most new AI products.",
            "Model choice is shifting from 'biggest' to 'best cost-quality fit'|Open-weight models narrow the gap with closed models|Evaluation and governance become procurement requirements",
            "Python,LLM application development,PyTorch", "US,CN,FR,GB", planned));
        list.add(tech("rag", "Retrieval-augmented generation", "AI", AI_DATA, ADOPT, 74, 80, 88, 26,
            "Grounding model output in an organisation's own documents and data at query time.",
            "Hybrid search (keyword + vector) becomes the default|Evaluation of retrieval quality is a new discipline|Moves from chatbots to embedded workflow features",
            "LLM application development,Python,SQL", "US,GB,IN,SG", planned));
        list.add(tech("small-language-models", "Small & on-device models", "AI", AI_DATA, TRIAL, 52, 78, 70, 35,
            "Compact models that run on laptops, phones and edge devices for privacy, latency and cost.",
            "Phone and PC makers ship dedicated NPUs|Distillation from larger models is routine|Strong fit for regulated industries",
            "PyTorch,Python,Edge deployment", "KR,US,CN,JP", planned));
        // AI Agents
        list.add(tech("coding-agents", "AI coding agents", "AI Agents", AI_DATA, TRIAL, 58, 95, 86, 62,
            "Agents that plan, edit, test and open pull requests across a codebase with human review.",
            "Shift from autocomplete to task-level delegation|Code review and testing become the bottleneck|Teams rewrite engineering guidelines for agent use",
            "AI agents,TypeScript,Python", "US,GB,IN", planned));
        list.add(tech("multi-agent-orchestration", "Multi-agent orchestration", "AI Agents", AI_DATA, ASSESS, 34, 84, 66, 58,
            "Coordinating several specialised agents with shared memory, tools and supervision.",
            "Frameworks consolidate quickly|Observability and cost control are unsolved|Best results come from narrow, well-scoped workflows",
            "AI agents,Python,LLM application development", "US,CN,GB", planned));
        list.add(tech("agent-protocols", "Agent tool protocols (e.g. MCP)", "AI Agents", PLATFORMS, TRIAL, 42, 88, 72, 70,
            "Open protocols that let agents discover and call tools, data sources and other agents in a standard way.",
            "Vendors expose products as agent-callable tools|Security review of tool permissions is critical|Becoming a de-facto integration layer",
            "AI agents,TypeScript,API design", "US,GB,SG", planned));
        // Software Engineering
        list.add(tech("typescript", "TypeScript everywhere", "Software Engineering", ENGINEERING, ADOPT, 88, 66, 86, 6,
            "Typed JavaScript across frontend, backend and tooling as the default for web product teams.",
            "Default for new web projects|Native type-stripping in runtimes|Strong fit with AI-assisted coding",
            "TypeScript,React,Node.js", "US,IN,GB,DE", planned));
        list.add(tech("rust", "Rust for systems & tooling", "Software Engineering", ENGINEERING, TRIAL, 60, 74, 62, 14,
            "Memory-safe systems language adopted for infrastructure, tooling, security-sensitive code and performance hot paths.",
            "Memory-safety guidance from security agencies|Rewrites of developer tooling in Rust|Growing use in kernels and embedded",
            "Rust,Systems programming", "US,DE,CN", planned));
        list.add(tech("wasm-server", "WebAssembly on the server", "Software Engineering", PLATFORMS, ASSESS, 36, 52, 38, 12,
            "Running sandboxed Wasm modules for plugins, edge functions and polyglot microservices.",
            "Component model matures|Edge platforms adopt Wasm runtimes|Tooling still uneven",
            "Rust,Go", "US,DE", planned));
        // Cloud
        list.add(tech("kubernetes", "Kubernetes", "Cloud", PLATFORMS, ADOPT, 90, 58, 84, 5,
            "The standard control plane for containerised workloads across clouds and on-premises.",
            "GPU scheduling for AI workloads|Abstracted behind internal platforms|Cost optimisation focus",
            "Kubernetes,Go,Terraform", "US,CN,DE,IN", planned));
        list.add(tech("serverless-containers", "Serverless containers", "Cloud", PLATFORMS, TRIAL, 64, 62, 64, 11,
            "Run containers without managing clusters, paying per request or per second.",
            "Cold-start improvements|Good fit for AI inference APIs|Lock-in trade-offs under review",
            "AWS,Kubernetes,Terraform", "US,GB,SG", planned));
        list.add(tech("sovereign-cloud", "Sovereign cloud", "Cloud", PLATFORMS, ASSESS, 40, 70, 58, 22,
            "Cloud regions and operating models that keep data, operations and keys under national or regional jurisdiction.",
            "Regulatory pressure in Europe and Asia|Hyperscaler-local partner models|Sovereign-AI compute programs",
            "Cloud architecture,Cybersecurity", "FR,DE,KR,JP", planned));
        // DevOps
        list.add(tech("platform-engineering", "Platform engineering & IDPs", "DevOps", PLATFORMS, TRIAL, 56, 72, 74, 18,
            "Internal developer platforms that give product teams paved roads for build, deploy and operate.",
            "Golden paths reduce cognitive load|Platform-as-a-product mindset|Agents become platform users",
            "Kubernetes,Terraform,Go", "US,GB,DE,IN", planned));
        list.add(tech("gitops", "GitOps", "DevOps", PLATFORMS, ADOPT, 76, 50, 66, 7,
            "Declarative infrastructure and application state reconciled from Git.",
            "Standard for Kubernetes delivery|Policy-as-code integration|Multi-cluster management",
            "Kubernetes,Terraform", "US,DE,GB", planned));
        list.add(tech("handrolled-ci", "Hand-rolled CI scripts", "DevOps", PLATFORMS, HOLD, 70, 12, 20, -8,
            "Bespoke, undocumented build scripts maintained by one person. Replace with shared, versioned pipelines.",
            "Supply-chain attestation needs reproducible builds|Hard for agents and newcomers to maintain",
            "DevOps", "Global", planned));
        // Cybersecurity
        list.add(tech("zero-trust", "Zero-trust architecture", "Cybersecurity", ENGINEERING, ADOPT, 72, 64, 82, 12,
            "Continuous verification of every user, device and workload instead of trusting the network perimeter.",
            "Identity becomes the perimeter|Required in many public-sector programs|Extends to AI agent identities",
            "Cybersecurity,Cloud architecture", "US,GB,SG,DE", planned));
        list.add(tech("post-quantum-crypto", "Post-quantum cryptography", "Cybersecurity", ENGINEERING, ASSESS, 30, 70, 54, 40,
            "Migrating key exchange and signatures to algorithms designed to resist future quantum attacks.",
            "Standardised algorithms available|Crypto-inventory projects start|'Harvest now, decrypt later' risk drives timelines",
            "Cybersecurity,Cryptography", "US,DE,FR,KR", planned));
        list.add(tech("supply-chain-security", "Software supply-chain security", "Cybersecurity", ENGINEERING, TRIAL, 54, 76, 74, 20,
            "SBOMs, signed artefacts, provenance attestations and dependency scanning across the build pipeline.",
            "Regulation requires SBOMs|Package-registry attacks continue|AI-generated code raises review needs",
            "Cybersecurity,DevOps", "US,GB,DE", planned));
        // Data Engineering
        list.add(tech("lakehouse", "Data lakehouse", "Data Engineering", AI_DATA, ADOPT, 74, 60, 76, 10,
            "Open table formats on object storage combining warehouse reliability with lake flexibility.",
            "Open table formats win interoperability|Feeds both BI and AI training|Governance catalogues consolidate",
            "SQL,Data engineering (Spark),Python", "US,IN,DE,GB", planned));
        list.add(tech("vector-databases", "Vector search & databases", "Data Engineering", AI_DATA, TRIAL, 50, 70, 70, 28,
            "Storing and searching embeddings for semantic retrieval, recommendations and RAG.",
            "Vector search added to general-purpose databases|Hybrid ranking matters more than engine choice",
            "SQL,Python,LLM application development", "US,CN,SG", planned));
        list.add(tech("streaming", "Real-time streaming", "Data Engineering", AI_DATA, ADOPT, 72, 54, 68, 8,
            "Event streams and stream processing for real-time analytics, features and operational data.",
            "Streaming feeds AI features|Managed services reduce ops load",
            "Data engineering (Spark),Java,SQL", "US,IN,DE", planned));
        // Robotics
        list.add(tech("humanoid-robots", "Humanoid robots", "Robotics", FRONTIER, ASSESS, 18, 86, 40, 55,
            "General-purpose humanoid platforms trained with foundation models for manipulation and mobility.",
            "Pilot deployments in logistics and manufacturing|Robot foundation models emerge|Unit economics still unproven",
            "Robotics (ROS),PyTorch,C++", "CN,US,JP,KR", planned));
        list.add(tech("amr", "Autonomous mobile robots", "Robotics", FRONTIER, TRIAL, 60, 62, 58, 18,
            "Warehouse, hospital and factory robots that navigate dynamic environments without fixed tracks.",
            "Labour shortages drive adoption|Fleet orchestration software matters",
            "Robotics (ROS),C++,Python", "JP,DE,CN,KR", planned));
        // Semiconductor
        list.add(tech("ai-accelerators", "AI accelerators", "Semiconductor", FRONTIER, ADOPT, 78, 90, 88, 34,
            "GPUs and custom silicon for AI training and inference; the central constraint on AI capacity.",
            "Custom chips from cloud providers|High-bandwidth memory is a bottleneck|Energy and cooling limits",
            "CUDA / GPU programming,C++,Hardware design", "US,KR,CN", planned));
        list.add(tech("chiplets", "Chiplets & advanced packaging", "Semiconductor", FRONTIER, TRIAL, 52, 74, 62, 24,
            "Combining multiple dies in one package to scale performance beyond single-die limits.",
            "Packaging capacity becomes strategic|Interconnect standards mature",
            "Hardware design", "US,KR,JP", planned));
        list.add(tech("leading-edge-nodes", "Leading-edge process nodes", "Semiconductor", FRONTIER, ASSESS, 44, 68, 56, 15,
            "Next-generation transistor architectures and lithography for the most advanced logic chips.",
            "New transistor architectures ramp|Fab investment tied to industrial policy",
            "Hardware design,Materials science", "KR,US,JP", planned));
        // Blockchain
        list.add(tech("rwa-tokenisation", "Tokenisation of real-world assets", "Blockchain", ENGINEERING, ASSESS, 30, 58, 42, 20,
            "Representing funds, bonds and other assets as programmable tokens on regulated ledgers.",
            "Regulated pilots by financial institutions|Clearer licensing regimes in some hubs",
            "Solidity,Cybersecurity", "SG,GB,US", planned));
        list.add(tech("speculative-tokens", "Speculative token projects", "Blockchain", ENGINEERING, HOLD, 40, 20, 18, -12,
            "Projects whose value proposition depends mainly on token price rather than a product.",
            "High regulatory and reputational risk|Talent shifts to infrastructure and compliance work",
            "Solidity", "Global", planned));
        // Emerging Technologies
        list.add(tech("quantum-computing", "Quantum computing", "Emerging Technologies", FRONTIER, ASSESS, 16, 64, 34, 22,
            "Early-stage quantum hardware and algorithms; error correction progress is the key milestone.",
            "Error-correction milestones|Cloud access to quantum hardware|Talent pool very small",
            "Python,Physics / quantum", "US,CN,DE,FR", planned));
        list.add(tech("spatial-computing", "Spatial computing", "Emerging Technologies", FRONTIER, ASSESS, 28, 46, 32, 9,
            "Mixed-reality headsets and 3D interfaces for training, design and remote collaboration.",
            "Enterprise training use cases|Device costs remain high",
            "C#,3D / graphics", "US,KR,JP", planned));
        list.add(tech("edge-ai", "Edge AI", "Emerging Technologies", FRONTIER, TRIAL, 48, 72, 60, 26,
            "Running inference on devices, vehicles and factory equipment close to where data is produced.",
            "NPUs in consumer and industrial devices|Privacy and latency drivers",
            "Edge deployment,PyTorch,C++", "KR,JP,CN,DE", planned));
        return list;
    }

    private static Technology tech(String slug, String name, String domain, RadarQuadrant q, RadarRing r,
                                   int maturity, int momentum, int demand, double growth, String description,
                                   String signals, String skills, String countries, String planned) {
        return new Technology(slug, name, domain, q, r, maturity, momentum, demand, growth, description, signals,
            skills, countries, sample(planned));
    }

    // ------------------------------------------------------------------ companies

    @Override
    public List<Company> companies() {
        String planned = "Company filings, official engineering blogs, job-posting APIs";
        return List.of(
            company("google", "Google", "US", "Mountain View", "Search, Cloud & AI",
                "Foundation models, Cloud, Custom AI chips, Search, Android", "foundation-models,ai-accelerators,kubernetes,quantum-computing",
                94, 82, 95, 92, 78,
                "Vertically integrated AI stack from custom accelerators and cloud infrastructure to models and consumer products.",
                "AI features across search and productivity|Custom accelerator roadmap|Cloud AI platform adoption", planned),
            company("microsoft", "Microsoft", "US", "Redmond", "Cloud, Productivity & AI",
                "Cloud, Developer tools, Enterprise AI assistants, Security", "foundation-models,coding-agents,zero-trust,kubernetes",
                95, 84, 90, 96, 80,
                "Enterprise distribution through cloud, productivity software and developer tools makes it a primary channel for AI adoption.",
                "AI assistants in productivity suites|Developer-tool agents|Security product consolidation", planned),
            company("amazon", "Amazon", "US", "Seattle", "Cloud & Commerce",
                "Cloud infrastructure, Custom silicon, Logistics robotics, AI services", "serverless-containers,ai-accelerators,amr,foundation-models",
                90, 86, 88, 93, 74,
                "Largest cloud infrastructure footprint plus warehouse robotics at scale; custom chips aim to lower AI cost.",
                "Custom training and inference chips|Warehouse robotics deployment|Managed AI model platform", planned),
            company("apple", "Apple", "US", "Cupertino", "Consumer Devices",
                "On-device AI, Custom silicon, Privacy, Spatial computing", "small-language-models,edge-ai,spatial-computing,chiplets",
                78, 70, 90, 94, 62,
                "Custom silicon and device ecosystem position it for privacy-focused, on-device AI.",
                "On-device model capabilities|Spatial computing platform|Silicon roadmap", planned),
            company("nvidia", "NVIDIA", "US", "Santa Clara", "Semiconductors & AI Platforms",
                "AI accelerators, Networking, CUDA software, Robotics simulation", "ai-accelerators,chiplets,humanoid-robots,edge-ai",
                97, 88, 94, 98, 86,
                "Dominant AI accelerator and software ecosystem; expanding into networking, simulation and robotics platforms.",
                "Next accelerator generation ramp|Robotics simulation platforms|Supply constraints in packaging and memory", planned),
            company("openai", "OpenAI", "US", "San Francisco", "AI Research & Products",
                "Frontier models, Assistants, Agents, Developer API", "foundation-models,coding-agents,multi-agent-orchestration,agent-protocols",
                96, 90, 92, 88, 88,
                "Frontier model developer with large consumer and developer platforms; focus on agents and reasoning models.",
                "Agent products for consumers and developers|Compute partnerships|Enterprise deployments", planned),
            company("meta", "Meta", "US", "Menlo Park", "Social Platforms & AI",
                "Open-weight models, Recommendation AI, Spatial computing, AI infrastructure", "foundation-models,spatial-computing,ai-accelerators",
                92, 78, 90, 86, 70,
                "Open-weight model strategy and huge recommendation workloads; large investments in AI infrastructure and mixed reality.",
                "Open-weight model releases|AI data-centre build-out|Wearables and mixed reality", planned),
            company("samsung", "Samsung", "KR", "Suwon", "Semiconductors & Electronics",
                "Memory, Foundry, Mobile devices, On-device AI", "ai-accelerators,leading-edge-nodes,edge-ai,small-language-models",
                82, 74, 92, 84, 66,
                "Memory and foundry businesses make it central to AI hardware supply; device business drives on-device AI.",
                "High-bandwidth memory roadmap|Foundry process competitiveness|On-device AI in phones", planned),
            company("alibaba", "Alibaba", "CN", "Hangzhou", "Cloud & Commerce",
                "Cloud, Open-weight models, E-commerce AI, Logistics", "foundation-models,kubernetes,rag,vector-databases",
                84, 76, 80, 86, 68,
                "Leading cloud provider in China with an active open-weight model family used widely by developers.",
                "Open-weight model family updates|Cloud AI services in Asia|Commerce AI agents", planned),
            company("tencent", "Tencent", "CN", "Shenzhen", "Platforms, Gaming & Cloud",
                "Messaging super-app, Gaming, Cloud, Enterprise AI", "foundation-models,cloud,ai-agents",
                80, 72, 82, 90, 64,
                "Super-app and gaming ecosystem with growing cloud and enterprise AI offerings.",
                "AI in messaging and mini-programs|Game-development AI tools|Enterprise cloud", planned),
            company("tsmc", "TSMC", "TW", "Hsinchu", "Semiconductor Foundry",
                "Leading-edge foundry, Advanced packaging, Specialty processes", "leading-edge-nodes,chiplets,ai-accelerators",
                88, 80, 96, 95, 76,
                "The leading contract chipmaker for advanced AI and mobile processors; advanced packaging capacity is strategic.",
                "Leading-edge node ramps|Advanced packaging capacity expansion|Overseas fab build-out", planned)
        );
    }

    private static Company company(String slug, String name, String hq, String city, String sector, String focus,
                                   String tech, int ai, int talent, int rnd, int eco, int hiring, String summary,
                                   String watch, String planned) {
        return new Company(slug, name, hq, city, sector, focus, tech, ai, talent, rnd, eco, hiring, summary, watch,
            sample(planned));
    }

    // ------------------------------------------------------------------ work market

    @Override
    public List<MarketSignal> marketSignals() {
        List<MarketSignal> list = new ArrayList<>();
        String jobs = "Job-posting aggregator APIs (planned)";

        // Remote jobs: share of postings that offer remote work, YoY change in points
        Object[][] remote = {
            {"Frontend engineering", 41.0, 1.5}, {"Backend engineering", 38.0, 0.8}, {"DevOps / SRE", 39.0, 1.2},
            {"Data engineering", 36.0, 2.1}, {"Mobile engineering", 35.0, -0.4}, {"AI / ML engineering", 33.0, 3.4},
            {"Security engineering", 29.0, 1.0}, {"Embedded / hardware", 12.0, -0.6}};
        for (Object[] r : remote) {
            list.add(new MarketSignal(MarketCategory.REMOTE_JOBS, (String) r[0], "Global", "Remote-eligible share of postings",
                (double) r[1], "%", (double) r[2], "Share of sample postings in this role family that allow fully remote work.",
                "remote", sample(jobs)));
        }
        Object[][] remoteRegion = {{"North America", 34.0}, {"Europe", 31.0}, {"Asia-Pacific", 22.0}, {"Latin America", 37.0}, {"Middle East & Africa", 19.0}};
        for (Object[] r : remoteRegion) {
            list.add(new MarketSignal(MarketCategory.REMOTE_JOBS, "Remote share - " + r[0], (String) r[0], "Remote-eligible share (region)",
                (double) r[1], "%", null, "Regional remote-eligible share across developer postings.", "remote,region", sample(jobs)));
        }

        // Freelance hourly rate bands (low/high USD)
        String freelance = "Freelance marketplace APIs (planned)";
        Object[][] fl = {
            {"AI / ML consulting", 95.0, 160.0}, {"Cloud architecture", 85.0, 140.0}, {"Cybersecurity", 75.0, 135.0},
            {"Blockchain / smart contracts", 70.0, 130.0}, {"Data engineering", 60.0, 120.0},
            {"Full-stack web", 45.0, 95.0}, {"Mobile apps", 45.0, 90.0}, {"UI / UX design", 40.0, 85.0}};
        for (Object[] f : fl) {
            list.add(new MarketSignal(MarketCategory.FREELANCE, (String) f[0], "Global", "Typical hourly rate band",
                (double) f[1], "USD/hr", (double) f[2], "Illustrative hourly band for experienced freelancers; varies widely by region and client.",
                "freelance", sample(freelance)));
        }

        // Developer salaries: median annual base (USD), software engineer & AI/ML engineer (secondary = AI/ML)
        String salary = "Salary survey datasets and statutory pay-transparency data (planned)";
        Object[][] sal = {
            {"US", 155000.0, 185000.0}, {"SG", 98000.0, 118000.0}, {"GB", 88000.0, 104000.0}, {"DE", 82000.0, 95000.0},
            {"FR", 66000.0, 76000.0}, {"JP", 60000.0, 72000.0}, {"KR", 56000.0, 68000.0}, {"CN", 52000.0, 66000.0},
            {"IN", 28000.0, 38000.0}};
        for (Object[] s : sal) {
            list.add(new MarketSignal(MarketCategory.SALARY, "Mid-level software engineer", (String) s[0], "Median annual base (USD)",
                (double) s[1], "USD", (double) s[2],
                "Primary value: mid-level software engineer. Secondary value: mid-level AI/ML engineer. Illustrative only; not adjusted for purchasing power.",
                "salary", sample(salary)));
        }

        // In-demand skills: demand score 0-100, YoY growth %
        Object[][] skills = {
            {"Python", 92.0, 6.0, "Language"}, {"LLM application development", 90.0, 38.0, "AI"},
            {"AWS", 88.0, 3.0, "Cloud"}, {"TypeScript", 86.0, 4.0, "Language"}, {"SQL", 85.0, 2.0, "Data"},
            {"React", 84.0, 2.0, "Frontend"}, {"Kubernetes", 82.0, 5.0, "Cloud"}, {"Java", 80.0, 1.0, "Language"},
            {"Cybersecurity", 80.0, 7.0, "Security"}, {"AI agents", 78.0, 55.0, "AI"}, {"PyTorch", 76.0, 14.0, "AI"},
            {"Spring Boot", 74.0, 2.0, "Backend"}, {"Terraform", 74.0, 6.0, "DevOps"},
            {"Data engineering (Spark)", 72.0, 5.0, "Data"}, {"Go", 70.0, 5.0, "Language"}, {"MLOps", 70.0, 16.0, "AI"},
            {"Rust", 62.0, 12.0, "Language"}, {"CUDA / GPU programming", 58.0, 22.0, "Hardware"}};
        for (Object[] s : skills) {
            list.add(new MarketSignal(MarketCategory.SKILL_DEMAND, (String) s[0], "Global", "Demand score (0-100)",
                (double) s[1], "score", (double) s[2], "Sample demand score with year-over-year growth in sample posting share.",
                (String) s[3], sample(jobs)));
        }

        // International projects (archetypes, not real tenders)
        String projects = "Public tender portals and partner networks (planned)";
        Object[][] proj = {
            {"Cross-border AI customer-support platform", "Europe", 6.0, 8.0, "Multilingual RAG assistant for a multi-country retailer. Remote-first team across EU time zones.", "LLM application development,Python,AWS"},
            {"Smart-factory data platform", "DE", 9.0, 12.0, "Streaming sensor data into a lakehouse for predictive maintenance.", "Data engineering (Spark),Kubernetes,SQL"},
            {"Fintech compliance automation", "SG", 5.0, 6.0, "Agent-assisted KYC document review with audit trails for a regional bank.", "AI agents,Java,Cybersecurity"},
            {"Public digital-services modernisation", "IN", 12.0, 20.0, "Migrating citizen services to cloud-native microservices.", "Spring Boot,Kubernetes,TypeScript"},
            {"Robotics fleet orchestration", "JP", 8.0, 10.0, "Scheduling and monitoring software for autonomous mobile robots in warehouses.", "Robotics (ROS),Go,Kubernetes"},
            {"Semiconductor yield analytics", "KR", 7.0, 6.0, "ML models on fab process data to detect yield excursions.", "PyTorch,Python,MLOps"},
            {"Zero-trust rollout for a hospital network", "GB", 6.0, 5.0, "Identity-centric access for clinical systems.", "Cybersecurity,Terraform"},
            {"Multilingual e-commerce search", "FR", 4.0, 4.0, "Hybrid keyword and vector search across European catalogues.", "Python,SQL,LLM application development"}};
        for (Object[] p : proj) {
            list.add(new MarketSignal(MarketCategory.INTERNATIONAL_PROJECT, (String) p[0], (String) p[1], "Typical engagement (months)",
                (double) p[2], "months", (double) p[3], "Sample project archetype (team size in secondary value). " + p[4],
                (String) p[5], sample(projects)));
        }
        return list;
    }

    // ------------------------------------------------------------------ trends

    @Override
    public List<TrendPoint> trendPoints() {
        List<TrendPoint> list = new ArrayList<>();
        String planned = "Job-posting APIs, GitHub activity, analyst indices (planned)";
        String[] years = {"2019", "2020", "2021", "2022", "2023", "2024", "2025", "2026"};

        Map<String, double[]> domains = new LinkedHashMap<>();
        domains.put("AI", new double[]{22, 26, 31, 38, 58, 74, 86, 95});
        domains.put("AI Agents", new double[]{2, 3, 4, 6, 14, 32, 60, 84});
        domains.put("Software Engineering", new double[]{60, 63, 66, 68, 70, 72, 75, 78});
        domains.put("Cloud", new double[]{48, 56, 63, 68, 72, 76, 80, 83});
        domains.put("DevOps", new double[]{44, 49, 54, 58, 61, 64, 67, 70});
        domains.put("Cybersecurity", new double[]{46, 52, 58, 63, 67, 71, 76, 80});
        domains.put("Data Engineering", new double[]{40, 45, 51, 56, 61, 66, 71, 75});
        domains.put("Robotics", new double[]{24, 26, 29, 32, 36, 42, 51, 60});
        domains.put("Semiconductor", new double[]{38, 42, 50, 54, 62, 74, 83, 89});
        domains.put("Blockchain", new double[]{30, 34, 52, 46, 34, 36, 37, 38});
        domains.put("Emerging Technologies", new double[]{18, 20, 23, 26, 29, 33, 38, 43});
        domains.forEach((key, values) -> addSeries(list, TrendSeriesType.TECHNOLOGY, key, years, values, "index", planned));

        addSeries(list, TrendSeriesType.AI_INDUSTRY, "Enterprise AI adoption", years,
            new double[]{18, 21, 25, 30, 42, 55, 66, 74}, "%", planned);
        addSeries(list, TrendSeriesType.AI_INDUSTRY, "AI investment index", years,
            new double[]{20, 22, 30, 32, 48, 66, 82, 92}, "index", planned);
        addSeries(list, TrendSeriesType.AI_INDUSTRY, "Agent deployments index", years,
            new double[]{1, 1, 2, 3, 8, 22, 48, 74}, "index", planned);

        addSeries(list, TrendSeriesType.DEVELOPER_MARKET, "Developer postings index", years,
            new double[]{70, 62, 92, 100, 78, 74, 80, 84}, "index", planned);
        addSeries(list, TrendSeriesType.DEVELOPER_MARKET, "Remote-eligible share", years,
            new double[]{9, 22, 34, 36, 33, 31, 30, 31}, "%", planned);
        addSeries(list, TrendSeriesType.DEVELOPER_MARKET, "AI skills share of postings", years,
            new double[]{4, 5, 6, 7, 11, 17, 24, 31}, "%", planned);

        String[] cYears = {"2021", "2022", "2023", "2024", "2025", "2026"};
        Map<String, double[]> countries = new LinkedHashMap<>();
        countries.put("US", new double[]{88, 89, 90, 92, 93, 94});
        countries.put("CN", new double[]{76, 78, 80, 82, 84, 86});
        countries.put("JP", new double[]{70, 71, 71, 72, 73, 74});
        countries.put("KR", new double[]{72, 74, 75, 77, 79, 80});
        countries.put("IN", new double[]{58, 61, 64, 67, 70, 72});
        countries.put("DE", new double[]{72, 73, 73, 74, 75, 76});
        countries.put("GB", new double[]{76, 77, 78, 79, 80, 81});
        countries.put("FR", new double[]{66, 68, 69, 71, 73, 74});
        countries.put("SG", new double[]{75, 77, 78, 80, 82, 83});
        countries.forEach((key, values) -> addSeries(list, TrendSeriesType.COUNTRY, key, cYears, values, "index", planned));
        return list;
    }

    private static void addSeries(List<TrendPoint> list, TrendSeriesType type, String key, String[] periods,
                                  double[] values, String unit, String planned) {
        for (int i = 0; i < periods.length; i++) {
            list.add(new TrendPoint(type, key, periods[i], values[i], unit, sample(planned)));
        }
    }

    // ------------------------------------------------------------------ reports

    @Override
    public List<IntelligenceReport> reports() {
        String planned = "Curated news / RSS feeds and analyst publications (planned)";
        return List.of(
            report("Agentic AI moves from pilots to production workflows", "AI Agents", "Global", 6,
                "Sample brief: organisations are moving agents from demos into narrow, auditable workflows such as code changes, support triage and document review.",
                "The pattern in this sample scenario is consistent: successful teams start with one well-bounded workflow, give the agent a small set of tools, and keep a human approval step before anything irreversible happens.|"
                    + "The constraint is shifting from model capability to surrounding engineering: evaluation suites, permission models for tools, cost monitoring and incident playbooks for agent mistakes.|"
                    + "Implication for practitioners: skills in API design, testing and observability compound in value when agents become the main consumers of internal systems.",
                "agents,automation,governance", "coding-agents,multi-agent-orchestration,agent-protocols", planned, LocalDate.of(2026, 11, 10)),
            report("AI compute capacity remains the binding constraint", "Semiconductor", "Global", 5,
                "Sample brief: accelerator supply, high-bandwidth memory and data-centre power are the three gating factors for AI capacity.",
                "Demand for training and inference capacity keeps outrunning supply in this sample outlook. The bottleneck moves between accelerators, advanced packaging, memory and grid connections.|"
                    + "Cloud providers respond with custom silicon and long-term power agreements, while countries frame compute as strategic infrastructure.|"
                    + "Watch: packaging capacity announcements, memory roadmaps, and data-centre permitting timelines.",
                "compute,chips,energy", "ai-accelerators,chiplets,leading-edge-nodes", planned, LocalDate.of(2026, 11, 7)),
            report("Sovereign AI programmes spread across Asia and Europe", "Policy", "Global", 7,
                "Sample brief: governments fund national compute, local-language models and sovereign cloud to reduce dependency on foreign platforms.",
                "Several economies in this sample scenario pair public compute investment with local-language model development and data-residency rules.|"
                    + "For multinational teams this means more regional deployment targets, stricter data-flow reviews and a premium on engineers who understand compliance.|"
                    + "Countries to watch in the dataset: France, Germany, South Korea, Japan, Singapore and India.",
                "policy,sovereignty,cloud", "sovereign-cloud,foundation-models", planned, LocalDate.of(2026, 11, 4)),
            report("Developer hiring shifts toward AI-fluent generalists", "Talent", "Global", 5,
                "Sample brief: postings increasingly ask for engineers who can combine solid fundamentals with practical AI tooling.",
                "In the sample job-market data, the share of developer postings mentioning AI skills keeps rising, while pure framework-specific roles grow slowly.|"
                    + "Employers emphasise system design, testing, code review and the ability to supervise AI-generated changes.|"
                    + "Career implication: pair one strong core stack (for example Java/Spring or TypeScript) with LLM application development and evaluation skills.",
                "talent,skills,hiring", "coding-agents,foundation-models,typescript", planned, LocalDate.of(2026, 10, 30)),
            report("Post-quantum migration becomes a board-level item", "Cybersecurity", "Global", 4,
                "Sample brief: crypto-inventory and migration planning start in finance, telecom and government.",
                "With standardised post-quantum algorithms available, the first step in this sample scenario is an inventory of where cryptography is used and which data must stay confidential for many years.|"
                    + "Hybrid key exchange is the common transitional approach. Vendors begin exposing PQC options in TLS libraries, HSMs and VPNs.|"
                    + "Skills in applied cryptography and secure protocol engineering remain scarce.",
                "security,cryptography", "post-quantum-crypto,zero-trust", planned, LocalDate.of(2026, 10, 27)),
            report("Robotics foundation models accelerate humanoid pilots", "Robotics", "CN", 6,
                "Sample brief: learned manipulation policies and simulation make humanoid pilots more common in logistics and manufacturing.",
                "Robotics teams in this sample scenario are adopting large pretrained policies and simulation-first training pipelines.|"
                    + "China, Japan, Korea and the US lead in pilot deployments. Unit economics, safety certification and reliability are still open questions.|"
                    + "Most near-term value is in autonomous mobile robots and fixed-cell manipulation rather than general-purpose humanoids.",
                "robotics,manufacturing", "humanoid-robots,amr", planned, LocalDate.of(2026, 10, 22)),
            report("Platform engineering absorbs DevOps tooling sprawl", "DevOps", "Europe", 4,
                "Sample brief: internal developer platforms consolidate CI/CD, infrastructure and observability into paved roads.",
                "Organisations in this sample scenario report fewer bespoke pipelines and more standardised golden paths maintained by a platform team.|"
                    + "AI agents are becoming a new class of platform user, which raises the bar for clear APIs, documentation and guardrails.",
                "devops,platform", "platform-engineering,gitops,kubernetes", planned, LocalDate.of(2026, 10, 18)),
            report("Remote work stabilises as a minority but durable share", "Work Market", "Global", 3,
                "Sample brief: remote-eligible developer postings have settled at roughly a third of the market in the sample data.",
                "After the 2020-2022 spike, the sample series shows remote eligibility stabilising rather than disappearing.|"
                    + "Cross-border hiring through employer-of-record services keeps remote roles accessible to strong candidates in Asia, Latin America and Eastern Europe.|"
                    + "Time-zone overlap and written communication skills are recurring requirements in remote postings.",
                "remote,hiring", "", planned, LocalDate.of(2026, 10, 14)),
            report("Korean memory makers ride the AI accelerator cycle", "Semiconductor", "KR", 4,
                "Sample brief: high-bandwidth memory is a strategic lever for South Korea's semiconductor industry.",
                "Accelerators depend on stacked high-bandwidth memory, and in this sample scenario Korean manufacturers hold a strong position.|"
                    + "Talent demand concentrates on packaging, process integration and hardware-software co-design.",
                "semiconductor,memory,korea", "ai-accelerators,chiplets", planned, LocalDate.of(2026, 10, 9)),
            report("Tokenised assets: regulated pilots, not speculation", "Blockchain", "SG", 4,
                "Sample brief: institutional blockchain work concentrates on tokenised funds and settlement under clear licensing.",
                "In this sample view, talent and investment move away from speculative tokens toward compliance-heavy infrastructure for regulated assets.|"
                    + "Hubs with clear regulatory frameworks attract pilots. Smart-contract security auditing remains in demand.",
                "blockchain,fintech", "rwa-tokenisation", planned, LocalDate.of(2026, 10, 3))
        );
    }

    private static IntelligenceReport report(String title, String category, String region, int minutes, String summary,
                                             String body, String tags, String tech, String planned, LocalDate date) {
        return new IntelligenceReport(title, category, region, minutes, summary, body, tags, tech, sample(planned, date));
    }

    // ------------------------------------------------------------------ sample news

    /**
     * Demo items for the "Latest Global Tech Updates" section. They describe broad, widely discussed
     * trends rather than specific events, carry no source URL (there is no original article), and are
     * stored with SAMPLE provenance so the UI never presents them as collected news.
     */
    @Override
    public List<NewsItem> sampleNews() {
        String planned = "Registered RSS / Atom feeds (see News Sources)";
        Object[][] rows = {
            {"Sample: Enterprises move from AI pilots to measured production rollouts", NewsCategory.AI, "Global", "Microsoft,Google", 70, 2,
                "Illustrative scenario: organisations prioritise AI use cases with measurable outcomes, adding evaluation and cost tracking before scaling."},
            {"Sample: Coding agents shift review effort from writing code to verifying it", NewsCategory.AI_AGENTS, "US", "OpenAI,Microsoft", 76, 1,
                "Illustrative scenario: teams adopting AI coding agents report that tests and code review become the main bottleneck, not authoring."},
            {"Sample: Agent tool protocols become a common integration layer", NewsCategory.AI_AGENTS, "Global", "", 68, 4,
                "Illustrative scenario: software vendors expose product capabilities as agent-callable tools with scoped permissions."},
            {"Sample: Humanoid robot pilots expand in logistics and manufacturing", NewsCategory.ROBOTICS, "CN", "NVIDIA", 64, 3,
                "Illustrative scenario: pilots focus on repetitive material handling; reliability and unit economics remain the open questions."},
            {"Sample: High-bandwidth memory remains a key constraint for AI accelerators", NewsCategory.SEMICONDUCTOR, "KR", "Samsung,NVIDIA", 72, 5,
                "Illustrative scenario: accelerator output depends on stacked memory and advanced packaging capacity as much as on logic chips."},
            {"Sample: Memory-safe languages gain ground in infrastructure code", NewsCategory.SOFTWARE_ENGINEERING, "Global", "", 52, 6,
                "Illustrative scenario: new infrastructure components are increasingly written in memory-safe languages such as Rust."},
            {"Sample: Platform teams standardise golden paths for AI workloads", NewsCategory.CLOUD_DEVOPS, "Europe", "Amazon", 55, 7,
                "Illustrative scenario: internal developer platforms add GPU scheduling, model serving templates and cost controls."},
            {"Sample: Organisations begin post-quantum cryptography inventories", NewsCategory.CYBERSECURITY, "Global", "", 66, 8,
                "Illustrative scenario: security teams catalogue where cryptography is used before migrating to post-quantum algorithms."},
            {"Sample: Lakehouse table formats converge as the default for AI-ready data", NewsCategory.DATA_ENGINEERING, "Global", "", 50, 9,
                "Illustrative scenario: open table formats on object storage serve both BI dashboards and AI training pipelines."},
            {"Sample: Earth-observation data becomes a building block for climate and insurance apps", NewsCategory.SPACE, "Global", "", 48, 10,
                "Illustrative scenario: more frequent satellite imagery enables applications in crop monitoring, insurance and disaster response."},
            {"Sample: Quantum error-correction milestones remain the key signal to watch", NewsCategory.EMERGING, "Global", "IBM,Google", 46, 11,
                "Illustrative scenario: progress is judged by error-corrected logical qubits rather than raw physical qubit counts."},
            {"Sample: Sovereign AI programmes pair national compute with local-language models", NewsCategory.AI, "FR", "", 58, 12,
                "Illustrative scenario: governments fund domestic compute capacity and local-language models to reduce platform dependency."}
        };
        List<NewsItem> list = new ArrayList<>();
        for (Object[] r : rows) {
            String title = (String) r[0];
            LocalDate date = COMPILED_ON.minusDays((int) r[5]);
            java.time.Instant at = date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            list.add(new NewsItem(title, sha256Hex("sample|" + title), title.toLowerCase(), (NewsCategory) r[1], (String) r[2],
                (String) r[3], (String) r[6], at, at, null, NewsSourceType.SAMPLE, (int) r[4], sample(planned, date)));
        }
        return list;
    }

    private static String sha256Hex(String s) {
        try {
            byte[] d = java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ------------------------------------------------------------------ learning paths

    @Override
    public List<LearningPath> learningPaths() {
        return List.of(
            new LearningPath("Python", "Python for production services", 6, "Typing and packaging|Testing with pytest|Async I/O and FastAPI|Profiling and performance"),
            new LearningPath("LLM application development", "Build reliable LLM features", 8, "Prompting and structured outputs|Retrieval-augmented generation|Evaluation sets and regression tests|Cost, latency and safety guardrails"),
            new LearningPath("AWS", "AWS core architect path", 8, "IAM, VPC and networking|Compute: EC2, containers, serverless|Storage and databases|Well-Architected reviews"),
            new LearningPath("TypeScript", "TypeScript in depth", 4, "Type system fundamentals|Generics and utility types|Strict mode migration|Typed APIs end to end"),
            new LearningPath("SQL", "Analytical and transactional SQL", 4, "Joins, window functions, CTEs|Indexing and query plans|Transactions and isolation|Data modelling"),
            new LearningPath("React", "Modern React", 5, "Components and hooks|State management patterns|Server rendering|Testing and accessibility"),
            new LearningPath("Kubernetes", "Kubernetes operator path", 8, "Pods, deployments, services|Config, secrets and storage|Helm and GitOps|Observability and autoscaling"),
            new LearningPath("Java", "Modern Java (21+)", 5, "Records, sealed types, pattern matching|Virtual threads|Streams and collections|JVM tuning basics"),
            new LearningPath("Cybersecurity", "Security engineering fundamentals", 10, "Threat modelling|OWASP Top 10 and secure coding|Identity and zero trust|Incident response basics"),
            new LearningPath("AI agents", "Designing AI agents", 6, "Tool calling and protocols|Planning and memory patterns|Human-in-the-loop approvals|Agent evaluation and observability"),
            new LearningPath("PyTorch", "Deep learning with PyTorch", 10, "Tensors and autograd|Training loops and data loaders|Fine-tuning pretrained models|Serving and quantisation"),
            new LearningPath("Spring Boot", "Spring Boot professional", 6, "Web MVC and validation|Spring Data JPA|Security and OAuth2|Testing, observability, deployment"),
            new LearningPath("Terraform", "Infrastructure as code", 4, "Providers and state|Modules and workspaces|Policy as code|CI pipelines for infra"),
            new LearningPath("Data engineering (Spark)", "Data engineering with Spark", 8, "Batch and streaming ETL|Lakehouse table formats|Orchestration|Data quality and lineage"),
            new LearningPath("Go", "Go for cloud services", 5, "Language essentials|Concurrency with goroutines|HTTP and gRPC services|Testing and profiling"),
            new LearningPath("MLOps", "MLOps foundations", 8, "Experiment tracking|Model registries and CI/CD|Monitoring and drift|Feature stores"),
            new LearningPath("Rust", "Rust from zero to systems", 10, "Ownership and borrowing|Traits and generics|Async Rust|FFI and performance"),
            new LearningPath("CUDA / GPU programming", "GPU programming essentials", 10, "GPU architecture basics|CUDA kernels and memory|Profiling|Using optimised libraries")
        );
    }
}
