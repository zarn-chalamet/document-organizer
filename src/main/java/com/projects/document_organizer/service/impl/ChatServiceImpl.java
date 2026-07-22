package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.DocumentChunkRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.ChatService;
import com.projects.document_organizer.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    private final RestTemplate restTemplate;

    private static final String PYTHON_CHAT_URL = "http://localhost:8000/chat";

    @Override
    public String chat(String question, String userEmail) {
        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 1 — Embed the question
        float[] questionEmbedding = embeddingService.embed(question);
        String vectorString = toVectorString(questionEmbedding);

        log.info("Searching for relevant chunks for user {}", userEmail);

        // Step 2 — Find top 5 similar chunks from user's documents only
        List<String> relevantChunks = documentChunkRepository
                .findTopSimilarChunks(user.getId(), vectorString);

        log.info("Found {} relevant chunks", relevantChunks.size());
        log.info("Relevant chunks: {}", relevantChunks); 

        // Step 3 — Send to Python /chat endpoint
        return callGroqChat(question, relevantChunks);
    }

    private String callGroqChat(String question, List<String> contextChunks) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "question", question,
                    "context_chunks", contextChunks
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_CHAT_URL, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("answer");
            }

            return "Sorry, I could not get a response. Please try again.";

        } catch (Exception e) {
            log.error("Chat failed: {}", e.getMessage());
            return "Sorry, the AI service is currently unavailable.";
        }
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
}