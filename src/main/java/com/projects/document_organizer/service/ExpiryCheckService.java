package com.projects.document_organizer.service;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryCheckService {

    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkDocumentExpiry() {

        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysFromNow = today.plusDays(30);

        // Get all documents expiring within 30 days AND already expired
        List<Document> docsToCheck = documentRepository
                .findByExpiryDateBefore(thirtyDaysFromNow);

        for (Document doc : docsToCheck) {

            if (doc.getExpiryDate() == null) {
                continue;
            }
            
            // Skip if already notified today
            if (doc.getLastNotifiedAt() != null &&
                    doc.getLastNotifiedAt().isEqual(today)) {
                continue;
            }

            long daysUntilExpiry = today.until(
                    doc.getExpiryDate(),
                    java.time.temporal.ChronoUnit.DAYS
            );

            // Only notify on these specific days
            boolean shouldNotify = daysUntilExpiry <= 0   // already expired
                    || daysUntilExpiry == 1
                    || daysUntilExpiry == 3
                    || daysUntilExpiry == 7
                    || daysUntilExpiry == 14
                    || daysUntilExpiry == 30;

            if (shouldNotify) {
                emailService.sendEmailNotification(doc);
                doc.setLastNotifiedAt(today);
                documentRepository.save(doc);
                log.info("Notified '{}' for document '{}', days until expiry: {}",
                        doc.getUser().getEmail(), doc.getTitle(), daysUntilExpiry);
            }
        }
    }
}
