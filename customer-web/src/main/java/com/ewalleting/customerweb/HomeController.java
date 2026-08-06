package com.ewalleting.customerweb;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shows the two tokens side by side, raw, on purpose — this is the "decode a JWT
 * by hand" exercise from the brief. Nothing here parses or explains the JWT for
 * you; that's the point.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(
            @AuthenticationPrincipal OidcUser oidcUser,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            Model model) {
        model.addAttribute("fullName", oidcUser.getFullName());
        model.addAttribute("username", oidcUser.getPreferredUsername());
        model.addAttribute("idTokenRaw", oidcUser.getIdToken().getTokenValue());
        model.addAttribute("idTokenClaims", oidcUser.getIdToken().getClaims());
        model.addAttribute("accessTokenRaw", authorizedClient.getAccessToken().getTokenValue());
        model.addAttribute("accessTokenExpiresAt", authorizedClient.getAccessToken().getExpiresAt());
        return "home";
    }
}
