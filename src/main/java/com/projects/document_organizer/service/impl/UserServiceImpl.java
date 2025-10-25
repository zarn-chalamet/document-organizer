package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.GoogleTokenService;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    public final GoogleTokenService googleTokenService;

    @Override
    @Transactional
    public User saveOrUpdateUserTokens(String email, String name, String accessToken, String refreshToken) {

        return userRepository.findByEmail(email).map(user -> {
            user.setName(name);
            if (accessToken != null) user.setGoogleAccessToken(accessToken);
            if (refreshToken != null) user.setGoogleRefreshToken(refreshToken);
            user.setLastTokenUpdatedAt(LocalDateTime.now());
            return userRepository.save(user);
        }).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .googleAccessToken(accessToken)
                    .googleRefreshToken(refreshToken)
                    .lastTokenUpdatedAt(LocalDateTime.now())
                    .build();
            return userRepository.save(newUser);
        });
    }

    @Override
    @Transactional
    public String getValidAccessToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        // naive check: if token is missing, try to refresh
        String accessToken = user.getGoogleAccessToken();
        if (accessToken == null || tokenIsExpired(user)) {
            if (user.getGoogleRefreshToken() == null) {
                throw new RuntimeException("No refresh token available for user: " + email);
            }
            // Refresh via GoogleTokenService
            GoogleTokenService.TokenResponse resp = googleTokenService.refreshAccessToken(user.getGoogleRefreshToken());
            user.setGoogleAccessToken(resp.getAccess_token());
            user.setLastTokenUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            return resp.getAccess_token();
        }
        return accessToken;
    }

    private boolean tokenIsExpired(User user) {
        LocalDateTime updated = user.getLastTokenUpdatedAt();
        if (updated == null) return true;
        return updated.isBefore(LocalDateTime.now().minusMinutes(50));
    }

}
