package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmailNotification(Document document) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            String subject;
            String body;
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

            if (document.getExpiryDate().isAfter(today)) {
                // Soon-to-expire reminder
                subject = "Reminder: Your document \"" + document.getTitle() + "\" will expire soon";
                body = """
                        <p>Dear %s,</p>
                        <p>This is a friendly reminder that your document <strong>%s</strong> is set to expire on <strong>%s</strong>.</p>
                        <p>Description: %s</p>
                        <p>Please take necessary action before it expires.</p>
                        <br>
                        <p>Best regards,<br>
                        <strong>Document Organizer Team</strong></p>
                        """.formatted(
                        document.getUser().getName(),
                        document.getTitle(),
                        document.getExpiryDate().format(formatter),
                        document.getDescription() != null ? document.getDescription() : "No description"
                );
            } else {
                // Already expired
                subject = "Alert: Your document \"" + document.getTitle() + "\" has expired";
                body = """
                        <p>Dear %s,</p>
                        <p>We wanted to inform you that your document <strong>%s</strong> expired on <strong>%s</strong>.</p>
                        <p>Description: %s</p>
                        <p>If necessary, please renew or update the document as soon as possible.</p>
                        <br>
                        <p>Best regards,<br>
                        <strong>Document Organizer Team</strong></p>
                        """.formatted(
                        document.getUser().getName(),
                        document.getTitle(),
                        document.getExpiryDate().format(formatter),
                        document.getDescription() != null ? document.getDescription() : "No description"
                );
            }

            helper.setTo(document.getUser().getEmail());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", document.getUser().getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", document.getUser().getEmail(), e.getMessage());
        }
    }
}
