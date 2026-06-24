package ovh.lukis.shortliner.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Profile("prd")
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
                        .requestMatchers("/", "/error", "/error/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/shorten/{shortCode}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/shorten").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/shorten/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/shorten").authenticated()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
