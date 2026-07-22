package com.projects.document_organizer.controller;

import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody Map<String, String> body) {
        String question = body.get("question");

        if (question == null || question.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question cannot be empty"));
        }

        String userEmail = getCurrentUserEmail();
        String answer = chatService.chat(question, userEmail);

        return ResponseEntity.ok(Map.of(
                "question", question,
                "answer", answer
        ));
    }

    private String getCurrentUserEmail() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getEmail();
    }
}