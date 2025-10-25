package com.projects.document_organizer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleTokenService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    private final RestTemplate rest = new RestTemplate();

    public TokenResponse refreshAccessToken(String refreshToken) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String,String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<TokenResponse> resp = rest.postForEntity(tokenUrl, request, TokenResponse.class);

        if (resp.getStatusCode() == HttpStatus.OK && resp.getBody() != null) {
            return resp.getBody();
        } else {
            throw new RuntimeException("Failed to refresh token: " + resp.getStatusCode());
        }
    }

    // Simple DTO for token response
    public static class TokenResponse {
        private String access_token;
        private Long expires_in;
        private String scope;
        private String token_type;
        // getters and setters
        public String getAccess_token() { return access_token; }
        public void setAccess_token(String access_token) { this.access_token = access_token; }
        public Long getExpires_in() { return expires_in; }
        public void setExpires_in(Long expires_in) { this.expires_in = expires_in; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public String getToken_type() { return token_type; }
        public void setToken_type(String token_type) { this.token_type = token_type; }
    }
}
