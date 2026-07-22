package com.projects.document_organizer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    @ToString.Exclude
    private Document document;

    // Which chunk this is (0, 1, 2...)
    @Column(nullable = false)
    private Integer chunkIndex;

    // The actual text content of this chunk
    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Column(columnDefinition = "vector(384)")
    private String embedding;

    private LocalDateTime createdAt;
}