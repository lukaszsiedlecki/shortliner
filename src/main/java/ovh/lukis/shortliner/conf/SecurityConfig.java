package ovh.lukis.shortliner.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Profile("!dev")
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: home page, static resources, error pages
                        .requestMatchers("/", "/error", "/error/**").permitAll()
                        // Public: redirect endpoint (GET /shorten/{shortCode})
                        .requestMatchers(HttpMethod.GET, "/shorten/{shortCode}").permitAll()
                        // Protected: create/delete/list endpoints
                        .requestMatchers(HttpMethod.POST, "/shorten").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/shorten/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/shorten").authenticated()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                }));

        return http.build();
    }
}
