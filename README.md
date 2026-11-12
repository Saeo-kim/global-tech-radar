# GLOBAL TECH RADAR

A global technology-intelligence dashboard (MVP): country capability, a technology radar, company
intelligence, the developer work market, a personal skill-gap analysis, analyst briefs — and
**Latest Global Tech Updates**, real technology news collected from 25 RSS/Atom feeds
(see [section 9](#9-latest-global-tech-updates-news-collection)).

**Stack:** Java 21 · Spring Boot 4.1 · Gradle 9 (wrapper included) · Spring Data JPA · MySQL 8 ·
Thymeleaf · vanilla HTML/CSS/JS · Chart.js 4 · jsVectorMap (world map)

> **Data notice.** Indicator data (countries, radar, companies, work market, reports) is a hand-authored
> **illustrative sample dataset**. No figure in it is measured or live, and none should be quoted. Every
> record is stored with `dataStatus = SAMPLE`, a source name, a publication date and the *planned*
> real-world source that would replace it, and the UI shows that on every data block.
> **News is different:** items labelled **Collected** are real articles fetched from public feeds
> (`dataStatus = LIVE`) with their original URL and date; 12 demo items are labelled **Sample**.

---

## 1. Requirements

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| MySQL | 8.x, running | `mysql --version` |
| Internet in the browser | for Chart.js, jsVectorMap and Google Fonts CDNs | — |

Gradle does **not** need to be installed — use the included wrapper (`gradlew` / `gradlew.bat`).

## 2. Configure MySQL (environment variables)

The database `global_tech_radar` is created automatically on first start
(`createDatabaseIfNotExist=true`), and tables are created/updated by Hibernate.

| Variable | Default | Meaning |
|---|---|---|
| `DB_HOST` | `localhost` | MySQL host |
| `DB_PORT` | `3306` | MySQL port |
| `DB_NAME` | `global_tech_radar` | Database (schema) name |
| `DB_USERNAME` | `root` | MySQL user |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `SERVER_PORT` | `8090` | HTTP port |
| `RADAR_DATA_PROVIDER` | `sample` | Active data provider id |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `RADAR_NEWS_COLLECTION` | `true` | Collect news from RSS/Atom feeds (`false` = offline) |
| `RADAR_NEWS_INTERVAL` | `PT1H` | Collection interval (ISO-8601 duration) |
| `RADAR_NEWS_TRUST_STORE` | `auto` | TLS trust store for feeds: `auto`, `os` or `jdk` |

**Windows PowerShell**
```powershell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-mysql-password"
.\gradlew.bat bootRun
```

**Git Bash / macOS / Linux**
```bash
export DB_USERNAME=root
export DB_PASSWORD='your-mysql-password'
./gradlew bootRun
```

Optional — a dedicated MySQL user instead of root:
```sql
CREATE DATABASE IF NOT EXISTS global_tech_radar CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'gtr'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON global_tech_radar.* TO 'gtr'@'localhost';
```
then set `DB_USERNAME=gtr` and `DB_PASSWORD=choose-a-password`.

## 3. Run

```bash
./gradlew bootRun          # Windows: .\gradlew.bat bootRun
```
Open **http://localhost:8090**. On first start the sample dataset (≈325 records) is seeded
automatically; the log shows `Seeded N intelligence records from provider 'sample'`.

**No MySQL at hand?** Run fully in memory (H2; data is lost on stop):
```bash
./gradlew bootRun --args='--spring.profiles.active=demo'
# PowerShell: .\gradlew.bat bootRun --args='--spring.profiles.active=demo'
```

**Runnable jar**
```bash
./gradlew bootJar
java -jar build/libs/global-tech-radar-0.1.0-MVP.jar
```

## 4. Test

```bash
./gradlew test
```
65 tests: data-rule checks on the sample provider (coverage, every record labelled `SAMPLE` with a
source and date, a learning path for every in-demand skill) and MockMvc integration tests that render
every page, hit the JSON API, register skills/goals, validate input, save settings and reload data —
plus the news feature: RSS/Atom/RDF parsing (incl. XXE rejection and odd date formats), classification,
the full collection pipeline against canned feeds (dedupe, failing feeds, disabled feeds, sample/live
separation), the HTTP fetcher against a local server (gzip, redirects, size cap, private-host refusal)
and the news pages / API.
Tests use an in-memory H2 database, so they do not need MySQL.

---

## 5. What's in the app

| Page | URL | Interactions |
|---|---|---|
| Dashboard | `/` | KPIs · interactive world map (metric switch, click country → indicator panel) · technology growth chart (pick up to 4 domains) · emerging technologies · AI industry trends · developer market · in-demand skills · sortable country indicator table · latest intelligence |
| Country Intelligence | `/countries`, `/countries/{code}` | USA, China, Japan, South Korea, India, Germany, UK, France, Singapore · map click toggles comparison · radar comparison (≤3) · index trend · card filter · detail page with profile radar vs average, trend, focus domains, leading technologies, HQ companies, salary |
| Technology Radar | `/technologies`, `/technologies/{slug}` | 11 domains (AI, AI Agents, Software Engineering, Cloud, DevOps, Cybersecurity, Data Engineering, Robotics, Semiconductor, Blockchain, Emerging Technologies) with sparklines · SVG radar (Adopt/Trial/Assess/Hold × 4 quadrants) · filter by domain, ring, search · click blip → detail panel · maturity-vs-momentum scatter · sortable table |
| Company Intelligence | `/companies`, `/companies/{slug}` | Google, Microsoft, Amazon, Apple, NVIDIA, OpenAI, Meta, Samsung, Alibaba, Tencent, TSMC · composite ranking · head-to-head radar (tick ≤3) · filter · detail with watch list, technologies, peers |
| Global Work Market | `/work-market` | Tabs: Remote Jobs · Freelance (rate bands + earnings calculator) · Developer Salaries (chart + sortable table) · In-demand Skills (sort by demand/growth) · International Projects (filterable archetypes) |
| My Technology Position | `/position` | Register skills (proficiency 1–5, years), projects, career goals (required skills, status) · market-alignment score · you-vs-market chart · prioritised learning recommendations with paths · full gap table · "Load example profile" |
| Global Tech Updates | `/news`, `/news/{id}`, `/news/sources` | Real news from RSS/Atom feeds · Latest Updates, Most Important News, Trending Technologies · keyword search, category / country / data-type filters, date or importance sort, paging · detail page with source facts separated from a 7-question automated analysis · source management (register, enable/disable, collect now, per-feed health). Also a panel on the Dashboard |
| Intelligence Reports | `/reports`, `/reports/{id}` | Search, category and region filters · brief detail with related technologies |
| Settings | `/settings` | Display name, home country, target role, accent colour, sample-banner toggle · active provider · reload data · API integration slots · JSON API links |

Global search is in the top bar (press <kbd>/</kbd>). The layout collapses to a slide-out sidebar
below 900 px.

### JSON API

`/api/meta` · `/api/countries` · `/api/countries/{code}` · `/api/technologies` · `/api/domains` ·
`/api/companies` · `/api/market/{remote_jobs|freelance|salary|skill_demand|international_project}` ·
`/api/trends/{technology|ai_industry|developer_market|country}?keys=a,b` · `/api/reports` ·
`/api/position/analysis` · `/api/search?q=` ·
`/api/news?q=&category=&region=&status=ALL|LIVE|SAMPLE&sort=NEWEST|OLDEST|IMPORTANCE&page=` ·
`/api/news/{id}` (returns `facts` and `interpretation` as separate objects) · `/api/news/trending` ·
`/api/news/sources`

Every data payload carries `provenance` (or `dataStatus`, `sourceName`, `publishedOn` for trends).

## 6. Architecture

```
com.globaltechradar
├── domain/       JPA entities; every intelligence entity embeds DataProvenance
│                 (dataStatus SAMPLE|LIVE, sourceName, sourceUrl, publishedOn, plannedSource)
├── provider/     IntelligenceDataProvider (interface) + SampleIntelligenceDataProvider
├── service/      DataIngestionService (seeds/reloads from the active provider)
│                 IntelligenceService (queries, aggregates, search)
│                 PositionService (profile CRUD + skill-gap analysis)
│                 SettingsService (key/value preferences)
│                 NewsService (news queries, trending, highlights)
│                 NewsAnalyzer → RuleBasedNewsAnalyzer (interpretation layer)
├── collector/    News collection, isolated from web code:
│                 HttpFeedFetcher → FeedParser → NewsClassifier → NewsCollectionService
│                 NewsSourceRegistry (config + user feeds), NewsCollectionScheduler
├── repository/   Spring Data JPA repositories
├── config/       RadarProperties (radar.data.*, radar.integrations[*]), NewsProperties (radar.news.*)
└── web/          Thymeleaf page controllers, GlobalModelAdvice, NewsController, ViewFormat, api/ REST
templates/        fragments/layout.html (head, sidebar, topbar, banner, provenance) + pages
static/           css/app.css (design tokens), js/app.js (shared helpers), js/pages/*.js
```

User-owned data (skills, projects, goals, settings) lives in separate tables and is never touched
when intelligence data is reloaded.

### Gap-analysis logic
For each in-demand market skill: `yourLevel = proficiency × 20`, `gap = max(0, demand − yourLevel)`,
`priority = gap × demand/100 + 0.4 × min(growth, 60) + 20 if a goal requires it`. Alignment score =
Σ min(level, demand) / Σ demand. Common aliases (k8s, golang, spark, llm, …) map to market skill names.

## 7. Adding a live data provider

1. Implement `IntelligenceDataProvider` (e.g. `WorldBankCountryProvider` calling
   `https://api.worldbank.org/v2`), annotate it `@Component`, and return records whose provenance is
   `new DataProvenance(DataStatus.LIVE, "<real source>", "<url>", <publication date>, null)`.
2. Give it a unique `id()` and start the app with `RADAR_DATA_PROVIDER=<id>`.
3. Add its connection settings under `radar.integrations` in `application.yml` (API keys via
   environment variables such as `JOBS_API_KEY`, `NEWS_API_KEY`, `GITHUB_TOKEN`) — they appear on
   the Settings page.
4. Use **Settings → Reload intelligence data** to re-ingest.

The sidebar badge, page banners and per-block provenance switch to "Live data" automatically for
records marked `LIVE`.

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `Access denied for user 'root'@'localhost'` | Set `DB_USERNAME` / `DB_PASSWORD` in the same terminal before `bootRun` |
| `Communications link failure` | MySQL is not running or `DB_HOST`/`DB_PORT` is wrong |
| `Port 8090 was already in use` | `SERVER_PORT=8091 ./gradlew bootRun` |
| Map or charts missing | The browser is offline / CDNs blocked; tables and lists still work |
| Flags show as two letters | Google Fonts (Noto Color Emoji) blocked; Windows has no built-in flag emoji |
| Every news feed fails with `PKIX path building failed` | Antivirus HTTPS scanning or a corporate proxy re-signs certificates. Keep `RADAR_NEWS_TRUST_STORE=auto` (Windows) or `os`, which validates against the Windows certificate store. For other OSes, import the proxy's root CA into the JDK trust store. Certificate validation is never disabled. |
| A news feed shows `Redirect … refused: HTTPS to HTTP downgrade` | The publisher redirects to plain HTTP. Register the HTTPS URL it ultimately serves (e.g. FeedBurner's `https://` address). |
| No news at all / want to work offline | `RADAR_NEWS_COLLECTION=false`; the 12 sample items still demonstrate the UI |

## 9. Latest Global Tech Updates (news collection)

### How it works
1. **Sources** are registered in `radar.news.feeds` (`application.yml`) and synced into the
   `news_source` table at startup, or added at runtime on **Global Tech Updates → Sources**.
2. **Collection** runs ~20 s after startup and then every `RADAR_NEWS_INTERVAL` (default 1 h), or on
   demand with **Collect now** (all feeds or a single feed). Runs never overlap.
3. **Fetch** (`HttpFeedFetcher`): HTTP(S) only, 20 s timeout, 5 MB cap (applied after gzip decoding),
   generic User-Agent, redirects followed manually with every hop re-validated, HTTPS→HTTP downgrades
   refused, loopback/private addresses refused (users can register URLs, so this prevents SSRF).
4. **Parse** (`FeedParser`): RSS 2.0, RSS 1.0/RDF and Atom with the JDK parser; DOCTYPE/external
   entities disabled (XXE). HTML is stripped; RFC-822 dates (including 2-digit years) and ISO-8601 parse.
5. **Classify** (`NewsClassifier`): category from keywords (headline ×3, summary ×1, feed default as
   head start and tie-break), region from country/city terms, companies from a list of ~50 names,
   importance 0–100 from source type, signal words, company mentions and category. All rule-based.
6. **De-duplicate**: SHA-256 of the normalised URL (lower-case host, no `www.`, tracking parameters
   and trailing slash removed) is a unique key; the same normalised headline within 7 days is also
   skipped (syndicated copies).
7. **Store** with `dataStatus = LIVE`, source name, **original URL** and **original publication date**.
   Each feed records status, last error, new/total items and consecutive failures. A failing feed never
   stops the others. Collected items older than `retention-days` (180) are purged.

### Facts vs interpretation
* **Facts** (green "Source facts" section): headline, source summary, original source and URL,
  publication date, detected region/companies/category.
* **Interpretation** (dashed purple "Automated interpretation · not from the source" section):
  answers *why it matters, affected industries, companies involved, business opportunities, should I
  learn/apply it, practical projects*. Produced by `RuleBasedNewsAnalyzer` from category templates,
  keywords in the text, Technology Radar values and the user's own skills. Nothing is stored; it is
  recomputed on view. To use an LLM instead, implement `NewsAnalyzer` as a `@Primary` bean that calls
  the model **server-side** with its key in an environment variable — keys never reach the browser.

### Registered sources (25)
| Type | Feeds |
|---|---|
| Official company blogs | Google AI, Google DeepMind, OpenAI, NVIDIA, Microsoft Research, AWS News, GitHub, Google Online Security, Cloudflare, Databricks, Kubernetes, Hugging Face |
| Research | MIT News (AI), MIT News (Robotics), arXiv cs.AI *(disabled by default: ~300 papers/day)* |
| Government / agencies | NASA, ESA, CISA advisories, NIST |
| Technology media | IEEE Spectrum, Semiconductor Engineering, The Robot Report, SpaceNews, InfoQ, Ars Technica |

### Adding a source permanently
```yaml
radar:
  news:
    feeds:
      - { id: my-lab, name: My Research Lab, url: "https://example.org/feed.xml",
          type: RESEARCH, category: AI, region: KR, company: "" }
```
`type`: `OFFICIAL_BLOG | RESEARCH | GOVERNMENT | MEDIA` ·
`category`: `AI | AI_AGENTS | ROBOTICS | SEMICONDUCTOR | SOFTWARE_ENGINEERING | CLOUD_DEVOPS | CYBERSECURITY | DATA_ENGINEERING | SPACE | EMERGING` ·
`region`: an ISO country code, `Europe` or `Global`. Add `enabled: false` to register without collecting.

### Limitations
* Feeds provide headlines and short summaries only; full article text is not fetched or stored.
* Category, region, companies and importance are keyword rules — usually right, sometimes not
  (e.g. a headline that only mentions "chips" metaphorically). Rules are re-applied to stored items at
  each startup, so improvements correct existing data.
* Interpretations are template-based, not an analyst's or an LLM's reading of the specific article.
* Some publishers (e.g. Anthropic) publish no RSS feed and cannot be added without an API or scraper.
