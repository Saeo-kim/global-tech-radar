package com.globaltechradar.service;

import com.globaltechradar.domain.Company;
import com.globaltechradar.domain.NewsItem;
import com.globaltechradar.domain.Technology;

import java.util.List;

/**
 * Produces the interpretation layer for a news item. Output is always an interpretation, never a fact,
 * and the UI renders it in a separately labelled section.
 * <p>
 * The MVP ships {@link RuleBasedNewsAnalyzer}. An LLM-backed implementation can be added later as a
 * {@code @Primary} bean (calling the model server-side, with its API key in an environment variable).
 */
public interface NewsAnalyzer {

    /** Human-readable name of the method, shown next to every interpretation. */
    String method();

    /** Short card-level interpretation. */
    Brief brief(NewsItem item);

    /** Full analysis for the detail page (questions 2-7; question 1 is answered by the source's own facts). */
    Analysis analyse(NewsItem item);

    record Brief(String impact, String applications) { }

    record Analysis(String method, String disclaimer, String whyItMatters, List<String> industries,
                    List<String> involvedCompanies, List<Company> trackedCompanies, List<String> opportunities,
                    String learnVerdict, List<String> learnReasons, List<String> projects,
                    List<Technology> relatedTechnologies) { }
}
