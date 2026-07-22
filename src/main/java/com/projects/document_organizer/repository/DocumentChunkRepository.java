package com.projects.document_organizer.repository;

import com.projects.document_organizer.model.DocumentChunk;
import com.projects.document_organizer.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    // Delete all chunks for a document (used when document is deleted)
    void deleteByDocument(Document document);

    // Find top 5 most similar chunks using cosine similarity
    // Filters by user's documents only (security)
    @Query(value = """
            SELECT dc.chunk_text
            FROM document_chunks dc
            JOIN documents d ON dc.document_id = d.id
            WHERE d.user_id = :userId
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<String> findTopSimilarChunks(
            @Param("userId") Long userId,
            @Param("embedding") String embedding
    );
}