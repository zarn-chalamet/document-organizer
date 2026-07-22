package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.repository.DocumentChunkRepository;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.service.EmbeddingService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentRepository documentRepository;
    private final RestTemplate restTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String PYTHON_EMBED_URL = "http://localhost:8000/embed";
    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Override
    public float[] embed(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of("text", text);
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_EMBED_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Double> embedding = (List<Double>) response.getBody().get("embedding");
                float[] result = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    result[i] = embedding.get(i).floatValue();
                }
                return result;
            }
            throw new RuntimeException("Empty response from embedding service");
        } catch (Exception e) {
            log.error("Failed to get embedding: {}", e.getMessage());
            throw new RuntimeException("Embedding failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void embedAndStoreDocument(Long documentId, String fullText) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        // Delete old chunks
        documentChunkRepository.deleteByDocument(document);

        List<String> chunks = splitIntoChunks(fullText);
        log.info("Splitting document {} into {} chunks", documentId, chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            if (chunkText.isBlank()) continue;

            try {
                float[] embeddingArray = embed(chunkText);

                // Convert float[] to pgvector string format: [0.1,0.2,0.3,...]
                String vectorString = toVectorString(embeddingArray);

                // Insert using native SQL to bypass Hibernate type issue
                entityManager.createNativeQuery("""
                        INSERT INTO document_chunks 
                        (chunk_index, chunk_text, created_at, document_id, embedding)
                        VALUES (:chunkIndex, :chunkText, :createdAt, :documentId, CAST(:embedding AS vector))
                        """)
                        .setParameter("chunkIndex", i)
                        .setParameter("chunkText", chunkText)
                        .setParameter("createdAt", LocalDateTime.now())
                        .setParameter("documentId", documentId)
                        .setParameter("embedding", vectorString)
                        .executeUpdate();

                log.info("Stored chunk {}/{} for document {}", i + 1, chunks.size(), documentId);

            } catch (Exception e) {
                log.error("Failed to embed chunk {} for document {}: {}", i, documentId, e.getMessage());
            }
        }

        log.info("Embedding complete for document {}", documentId);
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            sb.append(embedding[i]);
            if (i < embedding.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) return chunks;

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).trim());
            start += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }
}