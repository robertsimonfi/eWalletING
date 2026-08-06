package com.ewalleting.corefacade;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects any token not issued to the expected client (Keycloak's "azp" — authorized
 * party — claim). Without this, signature-only validation accepts *any* valid token
 * from the realm, including a customer-web user's own login token — proven live while
 * building this module (see NOTES.md). This is what actually restricts calls to
 * api-gateway's client-credentials token specifically.
 */
public class AuthorizedPartyValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAuthorizedParty;

    public AuthorizedPartyValidator(String expectedAuthorizedParty) {
        this.expectedAuthorizedParty = expectedAuthorizedParty;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        String azp = jwt.getClaimAsString("azp");
        if (expectedAuthorizedParty.equals(azp)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Token was not issued to the expected client (azp=" + azp + ")",
                null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
