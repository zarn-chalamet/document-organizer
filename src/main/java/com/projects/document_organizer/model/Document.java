package com.projects.document_organizer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

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

    private LocalDate expiryDate;

    private String driveFileId;     // Google Drive file ID
    private String driveFileLink;   // Web link for viewing
    private String fileType;        // e.g., PDF, JPG, PNG

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @ToString.Exclude // prevent circular reference when logging
    private User user;

}
