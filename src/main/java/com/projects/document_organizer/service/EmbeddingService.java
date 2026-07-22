package com.projects.document_organizer.service;


public interface EmbeddingService {

    // Convert text to 384-dimension vector via Python sidecar
    float[] embed(String text);

    // Split text into chunks and store embeddings in pgvector
    void embedAndStoreDocument(Long documentId, String fullText);
}