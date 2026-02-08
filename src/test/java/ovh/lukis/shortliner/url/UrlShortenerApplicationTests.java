package ovh.lukis.shortliner.url;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UrlShortenerApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testShortenUrl() throws Exception {
        String originalUrl = "https://www.example.com/some/long/url";
        String requestBody = String.format("{\"url\": \"%s\"}", originalUrl);

        mockMvc.perform(post("/shorten")
                        .with(jwt().jwt(j -> j.subject("test-user")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(originalUrl))
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    void testShortenUrlWithoutAuth() throws Exception {
        String requestBody = "{\"url\": \"https://www.example.com\"}";

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testRedirectIsPublic() throws Exception {
        // Redirect endpoint should be accessible without auth (returns 302 or error redirect)
        mockMvc.perform(get("/shorten/abc123"))
                .andExpect(status().is3xxRedirection());
    }
}
