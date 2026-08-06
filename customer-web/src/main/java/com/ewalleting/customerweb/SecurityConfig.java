package com.ewalleting.customerweb;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every page requires a login; hitting "/" with no session kicks off the
 * authorization-code + PKCE redirect to Keycloak automatically. This is the
 * user-facing OAuth2 flow — contrast with api-gateway's client-credentials
 * flow, which never involves a browser or a human at all.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(Customizer.withDefaults());
        return http.build();
    }
}
