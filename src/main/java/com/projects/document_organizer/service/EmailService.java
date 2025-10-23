package com.projects.document_organizer.service;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;

public interface EmailService {

    public void sendEmailNotification(Document document);
}
