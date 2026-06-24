package ovh.lukis.shortliner.url;

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
            return new ClickEventProducer(null) {
                @Override
                public void sendClickEvent(ClickEvent event) {
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

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
}
