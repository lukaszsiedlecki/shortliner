package ovh.lukis.shortliner.url;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ovh.lukis.shortliner.kafka.ClickEvent;
import ovh.lukis.shortliner.kafka.ClickEventProducer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UrlShortenerApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public ClickEventProducer clickEventProducer() {
            return new ClickEventProducer(null, null) {
                @Override
                public void sendClickEvent(ClickEvent event) {
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void testShortenUrl() throws Exception {
        String originalUrl = "https://www.example.com/some/long/url";
        String requestBody = String.format("{\"url\": \"%s\"}", originalUrl);

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(originalUrl))
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void testRedirectIsPublic() throws Exception {
        mockMvc.perform(get("/shorten/abc123"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void countsLinkCreationOutcomes() throws Exception {
        double successBefore = count("shortliner.links.created", "success");
        double duplicateBefore = count("shortliner.links.created", "duplicate");
        double invalidBefore = count("shortliner.links.created", "validation_error");
        String body = "{\"url\": \"https://www.example.com/metrics-test\"}";

        mockMvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/shorten").contentType(MediaType.APPLICATION_JSON).content("{\"url\": \"not a url\"}"))
                .andExpect(status().isBadRequest());

        assertThat(count("shortliner.links.created", "success")).isEqualTo(successBefore + 1);
        assertThat(count("shortliner.links.created", "duplicate")).isEqualTo(duplicateBefore + 1);
        assertThat(count("shortliner.links.created", "validation_error")).isEqualTo(invalidBefore + 1);
    }

    @Test
    void countsRedirectOutcomes() throws Exception {
        double hitBefore = count("shortliner.redirects", "hit");
        double notFoundBefore = count("shortliner.redirects", "not_found");
        String shortCode = com.jayway.jsonpath.JsonPath.read(mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://www.example.com/redirect-test\"}"))
                .andReturn().getResponse().getContentAsString(), "$.shortCode");

        mockMvc.perform(get("/shorten/" + shortCode)).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/shorten/nope00")).andExpect(status().is3xxRedirection());

        assertThat(count("shortliner.redirects", "hit")).isEqualTo(hitBefore + 1);
        assertThat(count("shortliner.redirects", "not_found")).isEqualTo(notFoundBefore + 1);
    }

    private double count(String name, String result) {
        var counter = meterRegistry.find(name).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }
}
