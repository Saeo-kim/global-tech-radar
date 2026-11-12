package com.globaltechradar;

import com.globaltechradar.service.DataIngestionService;
import com.globaltechradar.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired DataIngestionService ingestion;
    @Autowired PositionService position;

    @BeforeEach
    void seed() {
        ingestion.seedOnStartup();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/countries", "/countries/KR", "/technologies", "/technologies/coding-agents",
        "/technologies?domain=Cloud", "/companies", "/companies/nvidia", "/work-market", "/position", "/reports",
        "/reports?category=AI%20Agents", "/settings", "/search?q=ai"})
    void everyPageRendersAndIsLabelledAsSample(String url) throws Exception {
        mvc.perform(get(url))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("GLOBAL TECH RADAR")))
            .andExpect(content().string(containsString("Sample")));
    }

    @Test
    void reportDetailRenders() throws Exception {
        String json = mvc.perform(get("/api/reports")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Object id = com.jayway.jsonpath.JsonPath.read(json, "$[0].id");
        mvc.perform(get("/reports/" + id)).andExpect(status().isOk()).andExpect(content().string(containsString("Sample brief")));
    }

    @Test
    void unknownRecordsReturn404() throws Exception {
        mvc.perform(get("/countries/ZZ")).andExpect(status().isNotFound());
        mvc.perform(get("/technologies/does-not-exist")).andExpect(status().isNotFound());
        mvc.perform(get("/companies/nope")).andExpect(status().isNotFound());
    }

    @Test
    void apiCarriesProvenance() throws Exception {
        mvc.perform(get("/api/meta")).andExpect(status().isOk()).andExpect(jsonPath("$.dataStatus").value("SAMPLE"));
        mvc.perform(get("/api/countries")).andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(9)))
            .andExpect(jsonPath("$[0].provenance.dataStatus").value("SAMPLE"))
            .andExpect(jsonPath("$[0].provenance.publishedOn").exists());
        mvc.perform(get("/api/trends/technology")).andExpect(status().isOk())
            .andExpect(jsonPath("$.series", hasSize(11)))
            .andExpect(jsonPath("$.dataStatus").value("SAMPLE"));
        mvc.perform(get("/api/market/skill_demand")).andExpect(status().isOk()).andExpect(jsonPath("$", not(empty())));
        mvc.perform(get("/api/trends/not_a_type")).andExpect(status().isBadRequest());
    }

    @Test
    void skillRegistrationFeedsGapAnalysis() throws Exception {
        mvc.perform(post("/position/skills").param("name", "Kubernetes").param("category", "Cloud")
                .param("proficiency", "5").param("yearsExperience", "4"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(post("/position/goals").param("title", "Platform engineer").param("targetRole", "SRE")
                .param("targetRegion", "Global").param("targetDate", "2027-06-01").param("requiredSkills", "Go, Terraform"))
            .andExpect(status().is3xxRedirection());

        PositionService.Analysis a = position.analyse();
        assertThat(a.comparisons()).anySatisfy(c -> {
            assertThat(c.skill()).isEqualTo("Kubernetes");
            assertThat(c.userLevel()).isEqualTo(100);
            assertThat(c.status()).isEqualTo("Strong");
        });
        assertThat(a.goalSkillsMissing()).contains("Go", "Terraform");
        assertThat(a.recommendations()).isNotEmpty().allSatisfy(r -> assertThat(r.path()).isNotNull());

        mvc.perform(get("/api/position/analysis")).andExpect(status().isOk()).andExpect(jsonPath("$.alignmentScore").isNumber());
    }

    @Test
    void invalidSkillIsRejected() throws Exception {
        mvc.perform(post("/position/skills").param("name", "").param("proficiency", "9"))
            .andExpect(status().is3xxRedirection())
            .andExpect(flash().attributeExists("toastError"));
    }

    @Test
    void settingsSaveAndReload() throws Exception {
        mvc.perform(post("/settings").param("displayName", "Test Analyst").param("homeCountry", "kr")
                .param("targetRole", "AI engineer").param("accent", "violet").param("showSampleBanner", "true"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/")).andExpect(content().string(containsString("data-accent=\"violet\"")));
        mvc.perform(post("/settings/reload-data")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/countries")).andExpect(jsonPath("$", hasSize(9)));
    }
}
