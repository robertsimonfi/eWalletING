package com.ewalleting.apigateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Adding spring-boot-starter-security-oauth2-client (for the outbound
 * client-credentials call) pulls in Spring Security, which by default locks down
 * every endpoint. That default is for customer-web's browser login, not for this
 * gateway — incoming requests here aren't authenticated at all yet (that arrives
 * with the wallet/verifier modules); access control on the *downstream* call is
 * core-facade-rest's job as a resource server, not the gateway's.
 */
@Configuration
public class GatewaySecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
