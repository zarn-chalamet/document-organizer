package com.projects.document_organizer.security;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuth2AuthorizedClientService authorizedClientService;
    private final UserService userService;
    private final JwtService jwtService;

    @Value("${app.frontend.redirect-url}")
    private String frontendRedirect;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        // Load authorized client safely
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());

        String accessToken = null;
        String refreshToken = null;
        if (client != null) {
            if (client.getAccessToken() != null)
                accessToken = client.getAccessToken().getTokenValue();
            if (client.getRefreshToken() != null)
                refreshToken = client.getRefreshToken().getTokenValue();
        }

        // Debug logging (remove later)
        System.out.println("OAuth2 login success for: " + email);
        System.out.println("Access token: " + (accessToken != null ? "✔" : "✖"));
        System.out.println("Refresh token: " + (refreshToken != null ? "✔" : "✖"));

        // Save or update user tokens
        User user = userService.saveOrUpdateUserTokens(email, name, accessToken, refreshToken);

        // Generate app JWT
        String jwt = jwtService.generateToken(email);

        // Redirect with JWT and email
        String redirectUrl = frontendRedirect + "#token=" + jwt + "&email=" + email;
        response.sendRedirect(redirectUrl);
    }
}
