package hr.algebra.cloudbased_inventory_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // For a stateless REST API
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Permit everything for now (MVP). Tighten later when JWT is added.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/**", "/actuator/**", "/h2-console/**").permitAll()
                        .anyRequest().permitAll()
                )

                // No login UIs or basic auth for now
                .httpBasic(Customizer.withDefaults())
                .formLogin(form -> form.disable())

                // If you use H2 console
                .headers(h -> h.frameOptions(f -> f.disable()));

        return http.build();
    }
}
