package com.globaltechradar;

import com.globaltechradar.repository.NewsItemRepository;
import com.globaltechradar.service.DataIngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NewsPagesTest {

    @Autowired MockMvc mvc;
    @Autowired DataIngestionService ingestion;
    @Autowired NewsItemRepository news;

    @BeforeEach
    void seed() {
        ingestion.seedOnStartup();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/news", "/news?category=SEMICONDUCTOR", "/news?region=KR", "/news?status=SAMPLE",
        "/news?status=LIVE", "/news?sort=IMPORTANCE", "/news?sort=OLDEST", "/news?q=agents", "/news?page=99",
        "/news?category=bogus&sort=bogus", "/news/sources"})
    void newsPagesRender(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isOk())
            .andExpect(content().string(containsString("Global Tech Updates")));
    }

    @Test
    void listShowsRequiredComponentsAndLabelsSampleItems() throws Exception {
        mvc.perform(get("/news"))
            .andExpect(status().isOk())
            .andExpect(content().string(allOf(
                containsString("Latest Updates"), containsString("Trending Technologies"), containsString("Most Important News"),
                containsString("Last successful collection"), containsString("No original article (sample)"),
                containsString("Automated interpretation"))));
    }

    @Test
    void sampleItemsAreNeverLabelledCollected() throws Exception {
        // Only sample items exist in this context, so no card may carry the "Collected" badge.
        mvc.perform(get("/news?status=SAMPLE"))
            .andExpect(content().string(not(containsString("title=\"Collected from a registered RSS/Atom feed\""))));
    }

    @Test
    void filtersNarrowResults() throws Exception {
        mvc.perform(get("/news?category=SPACE"))
            .andExpect(content().string(containsString("Earth-observation")))
            .andExpect(content().string(not(containsString("Memory-safe languages gain ground"))));
        mvc.perform(get("/news?status=LIVE")).andExpect(content().string(containsString("No updates match these filters")));
        mvc.perform(get("/news?q=quantum")).andExpect(content().string(containsString("Quantum error-correction")));
    }

    @Test
    void detailSeparatesFactsFromInterpretation() throws Exception {
        Long id = news.findAll().get(0).getId();
        mvc.perform(get("/news/" + id))
            .andExpect(status().isOk())
            .andExpect(content().string(allOf(
                containsString("Source facts"), containsString("1. What happened?"),
                containsString("Automated interpretation"), containsString("2. Why does it matter?"),
                containsString("3. Which industries are affected?"), containsString("4. Which companies are involved?"),
                containsString("5. What business opportunities could emerge?"), containsString("6. Should I learn or apply this technology?"),
                containsString("7. What practical projects could be built?"), containsString("Sample item."))));
        mvc.perform(get("/news/999999")).andExpect(status().isNotFound());
    }

    @Test
    void apiSeparatesFactsAndInterpretation() throws Exception {
        Long id = news.findAll().get(0).getId();
        mvc.perform(get("/api/news/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.facts.title").exists())
            .andExpect(jsonPath("$.facts.provenance.dataStatus").value("SAMPLE"))
            .andExpect(jsonPath("$.interpretation.method").exists())
            .andExpect(jsonPath("$.interpretation.projects").isArray());
        mvc.perform(get("/api/news?category=AI&sort=IMPORTANCE")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.lastSuccessfulCollection").exists());
        mvc.perform(get("/api/news/trending")).andExpect(status().isOk());
        mvc.perform(get("/api/news/sources")).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].url", hasItem("https://openai.com/news/rss.xml")));
    }

    @Test
    void invalidSourceRegistrationIsRejectedGracefully() throws Exception {
        mvc.perform(post("/news/sources").param("name", "Bad").param("url", "file:///etc/passwd"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("toastError"));
        mvc.perform(post("/news/sources/does-not-exist/toggle"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("toastError"));
    }

    @Test
    void dashboardShowsLatestUpdatesPanel() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(allOf(
            containsString("Latest Global Tech Updates"), containsString("last successful collection"))));
    }
}
