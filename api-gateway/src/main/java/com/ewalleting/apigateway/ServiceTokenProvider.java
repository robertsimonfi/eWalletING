package com.ewalleting.apigateway;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

/**
 * Gets the gateway its own access token via client-credentials — no user, no
 * browser, no session. This is the other OAuth2 flow: the gateway proves *itself*
 * to Keycloak, gets a token representing "api-gateway", and that's what
 * core-facade-rest checks, completely independent of whatever a customer-web
 * session is doing.
 */
@Component
public class ServiceTokenProvider {

    private static final String REGISTRATION_ID = "core-facade";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public ServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(REGISTRATION_ID)
                .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(request);
        if (authorizedClient == null) {
            throw new IllegalStateException("Could not obtain a client-credentials token for " + REGISTRATION_ID);
        }
        return authorizedClient.getAccessToken().getTokenValue();
    }
}
