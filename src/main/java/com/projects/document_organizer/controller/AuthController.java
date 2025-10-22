package com.projects.document_organizer.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.projects.document_organizer.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
public class AuthController {

    @Value("${google.client.id}")
    private String CLIENT_ID;

    @Value("${google.client.secret}")
    private String CLIENT_SECRET;

    private final UserService userService;

    private static final String REDIRECT_URI = "http://localhost:8080/v1/api/oauth/callback";

    @GetMapping("/oauth/google")
    public void googleAuth(HttpServletResponse response) throws IOException {

        String redirectUri = URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8);

        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + CLIENT_ID
                + "&redirect_uri=" + redirectUri
                + "&response_type=code"
                + "&scope=" + URLEncoder.encode(
                "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile openid",
                StandardCharsets.UTF_8)
                + "&access_type=offline"
                + "&prompt=consent";

        response.sendRedirect(url);
    }

    @GetMapping("/oauth/callback")
    public ResponseEntity<?> oauthCallback(@RequestParam("code") String code) throws IOException {
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                CLIENT_ID,
                CLIENT_SECRET,
                code,
                REDIRECT_URI
        ).execute();

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // Fetch user info from Google API
        var transport = new NetHttpTransport();
        var requestFactory = transport.createRequestFactory();
        var url = new com.google.api.client.http.GenericUrl("https://www.googleapis.com/oauth2/v2/userinfo");
        var request = requestFactory.buildGetRequest(url);
        request.getHeaders().setAuthorization("Bearer " + accessToken);
        var response = request.execute();
        var json = response.parseAsString();

        // Parse manually (or use Jackson)
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var userInfo = mapper.readTree(json);
        String email = userInfo.get("email").asText();
        String name = userInfo.get("name").asText();

        // Save tokens for this user
        userService.saveOrUpdateGoogleTokens(email, name, accessToken, refreshToken);

        return ResponseEntity.ok("Google Drive connected successfully for " + name + "!");
    }
}
