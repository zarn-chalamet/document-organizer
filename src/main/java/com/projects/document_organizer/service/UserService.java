package com.projects.document_organizer.service;

import com.projects.document_organizer.model.User;

public interface UserService {

    public User saveOrUpdateGoogleTokens(String email, String name, String accessToken, String refreshToken);
}
