package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.respository.DocumentRepository;
import com.projects.document_organizer.service.EmailService;
import com.projects.document_organizer.service.ExpiryCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryCheckServiceImpl implements ExpiryCheckService {

    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    // Run every day at 09:00 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDocumentExpiry() {

        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        // Find documents expiring soon or expired
        List<Document> expiringDocs = documentRepository.findByExpiryDateBetween(today, threeDaysFromNow);
        List<Document> expiredDocs = documentRepository.findByExpiryDateBefore(today);

        for (Document doc : expiringDocs) {
            log.info("Document '{}' will expire soon (on {}).", doc.getTitle(), doc.getExpiryDate());

            //send email to the user
            emailService.sendEmailNotification(doc);

        }

        for (Document doc : expiredDocs) {
            log.warn("Document '{}' has already expired (on {}).", doc.getTitle(), doc.getExpiryDate());

            //send email to the user
            emailService.sendEmailNotification(doc);
        }
    }
}
