package com.projects.document_organizer.service;

import com.projects.document_organizer.model.User;

public interface UserService {

    public User saveOrUpdateUserTokens(String email, String name, String accessToken, String refreshToken);

    public String getValidAccessToken(String email);
}
