package com.ewalleting.corefacade;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Every /api/** call must carry a valid Bearer JWT signed by Keycloak — this is
 * what turns api-gateway's client-credentials token from "a string it sends" into
 * something actually enforced. Uses jwk-set-uri (signature verification only), not
 * issuer-uri — see NOTES.md for why, and what that trades away.
 */
@Configuration
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    // jwk-set-uri alone only proves the token is validly signed by this realm — it
    // says nothing about *which* client it was issued to. Without this, a
    // customer-web user's own login token passes just as easily as api-gateway's
    // service token (proven live — see NOTES.md). The azp check is what actually
    // narrows this down to "only api-gateway may call this service."
    @Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withTimestamp = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> withAuthorizedParty = new AuthorizedPartyValidator("api-gateway");
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withTimestamp, withAuthorizedParty));
        return jwtDecoder;
    }
}
