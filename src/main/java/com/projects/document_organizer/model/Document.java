package com.projects.document_organizer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    // Nullable — filled by AI later
    private LocalDate expiryDate;

    private LocalDate lastNotifiedAt;

    private String driveFileId;
    private String driveFileLink;
    private String fileType;

    // AI Scan Fields 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ScanStatus scanStatus = ScanStatus.PENDING;

    // Raw OCR text — stays local, never sent to cloud
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    // Auto-extracted expiry date — user can confirm or override
    private LocalDate extractedExpiryDate;

    // Detected document type from OCR (Passport, Visa, etc.)
    private String detectedDocumentType;

    // When OCR was last attempted
    private LocalDateTime scannedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @ToString.Exclude
    @JsonIgnore
    private Category category;
}