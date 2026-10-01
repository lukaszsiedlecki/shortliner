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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import ovh.lukis.shortliner.kafka.ClickEvent;
import ovh.lukis.shortliner.kafka.ClickEventProducer;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                    SENT_EVENTS.add(event);
                }
            };
        }
    }

    private static final List<ClickEvent> SENT_EVENTS = new CopyOnWriteArrayList<>();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private UrlRepository urlRepository;

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

    @Test
    void anonymousShortenHasNoOwner() throws Exception {
        String shortCode = shorten("https://www.example.com/anonymous", null);

        assertThat(urlRepository.findByShortCode(shortCode)).get()
                .extracting(UrlEntity::getOwnerId).isNull();
    }

    @Test
    void authenticatedShortenStoresSubjectAsOwner() throws Exception {
        String owner = UUID.randomUUID().toString();

        String shortCode = shorten("https://www.example.com/owned", owner);

        assertThat(urlRepository.findByShortCode(shortCode)).get()
                .extracting(UrlEntity::getOwnerId).isEqualTo(owner);
    }

    @Test
    void sameUrlIsDedupedPerOwner() throws Exception {
        String url = "https://www.example.com/dedupe-per-owner";
        String anonymousCode = shorten(url, null);
        String ownedCode = shorten(url, UUID.randomUUID().toString());

        assertThat(ownedCode).isNotEqualTo(anonymousCode);
        assertThat(shorten(url, null)).isEqualTo(anonymousCode);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/shorten")).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsOnlyCallersLinks() throws Exception {
        String owner = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();
        String ownCode = shorten("https://www.example.com/mine", owner);
        shorten("https://www.example.com/theirs", other);
        shorten("https://www.example.com/nobodys", null);

        mockMvc.perform(get("/shorten").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].shortCode").value(ownCode));
    }

    @Test
    void deleteRequiresAuthentication() throws Exception {
        String shortCode = shorten("https://www.example.com/delete-anon", null);

        mockMvc.perform(delete("/shorten/" + shortCode)).andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanDeleteOwnLink() throws Exception {
        String owner = UUID.randomUUID().toString();
        String shortCode = shorten("https://www.example.com/delete-own", owner);
        // Warm the cache so the test also proves delete evicts it.
        mockMvc.perform(get("/shorten/" + shortCode)).andExpect(status().is3xxRedirection());

        mockMvc.perform(delete("/shorten/" + shortCode).with(user(owner))).andExpect(status().isNoContent());

        assertThat(urlRepository.findByShortCode(shortCode)).isEmpty();
        mockMvc.perform(get("/shorten/" + shortCode))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/error"));
    }

    @Test
    void deletingSomeoneElsesLinkReturns404() throws Exception {
        String shortCode = shorten("https://www.example.com/delete-foreign", UUID.randomUUID().toString());

        mockMvc.perform(delete("/shorten/" + shortCode).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/shorten/" + shortCode).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        assertThat(urlRepository.findByShortCode(shortCode)).isPresent();
    }

    @Test
    void deletingAnonymousLinkAsUserReturns404() throws Exception {
        String shortCode = shorten("https://www.example.com/delete-unowned", null);

        mockMvc.perform(delete("/shorten/" + shortCode).with(user(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteAnyLink() throws Exception {
        String shortCode = shorten("https://www.example.com/delete-by-admin", UUID.randomUUID().toString());

        mockMvc.perform(delete("/shorten/" + shortCode).with(jwt()
                        .jwt(j -> j.subject(UUID.randomUUID().toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_user"), new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isNoContent());
        assertThat(urlRepository.findByShortCode(shortCode)).isEmpty();
    }

    @Test
    void redirectIsAnonymousAndClickEventCarriesLinkOwner() throws Exception {
        String owner = UUID.randomUUID().toString();
        String shortCode = shorten("https://www.example.com/click-owner", owner);
        String anonymousCode = shorten("https://www.example.com/click-anonymous", null);

        mockMvc.perform(get("/shorten/" + shortCode)).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/shorten/" + anonymousCode)).andExpect(status().is3xxRedirection());

        assertThat(SENT_EVENTS).filteredOn(e -> e.shortCode().equals(shortCode))
                .singleElement().extracting(ClickEvent::userId).isEqualTo(owner);
        assertThat(SENT_EVENTS).filteredOn(e -> e.shortCode().equals(anonymousCode))
                .singleElement().extracting(ClickEvent::userId).isNull();
    }

    @Test
    void unknownRouteStays404() throws Exception {
        mockMvc.perform(get("/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void healthIsPublicButOtherActuatorEndpointsNeedAdmin() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/metrics").with(user(UUID.randomUUID().toString())))
                .andExpect(status().isForbidden());
    }

    private String shorten(String url, String ownerId) throws Exception {
        var request = post("/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"url\": \"%s\"}", url));
        if (ownerId != null) {
            request.with(user(ownerId));
        }
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.shortCode");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor user(String subject) {
        return jwt().jwt(j -> j.subject(subject)).authorities(new SimpleGrantedAuthority("ROLE_user"));
    }
}
