package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.respository.UserRepository;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.UserPrincipalNotFoundException;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User saveOrUpdateGoogleTokens(String email, String name, String accessToken, String refreshToken) {

        User user = userRepository.findByEmail(email);
        if(user != null) {
            user.setName(name);
            user.setGoogleAccessToken(accessToken);
            user.setGoogleRefreshToken(refreshToken);

            userRepository.save(user);

            return user;
        }

        User newUser = User.builder()
                .email(email)
                .name(name)
                .googleAccessToken(accessToken)
                .googleRefreshToken(refreshToken)
                .build();

        return userRepository.save(newUser);
    }
}
