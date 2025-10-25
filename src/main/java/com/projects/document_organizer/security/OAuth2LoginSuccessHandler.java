package com.projects.document_organizer.security;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
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
    private final String frontendRedirect = "http://localhost:3000/oauth-success"; // or from properties

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String email = (String) oauthUser.getAttribute("email");
        String name = (String) oauthUser.getAttribute("name");

        // Load authorized client to get tokens
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient("google", authentication.getName());
        String accessToken = client.getAccessToken() != null ? client.getAccessToken().getTokenValue() : null;
        String refreshToken = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;

        User user = userService.saveOrUpdateUserTokens(email, name, accessToken, refreshToken);

        // Issue application JWT (subject = email)
        String jwt = jwtService.generateToken(email);

        // redirect to frontend with token (fragment)
        String redirectUrl = frontendRedirect + "#token=" + jwt + "&email=" + email;
        response.sendRedirect(redirectUrl);
    }
}
