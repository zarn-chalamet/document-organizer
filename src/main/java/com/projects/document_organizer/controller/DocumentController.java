package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    //Test
    @GetMapping("/test")
    public ResponseEntity<?> testing(){
        System.out.println("testing phase");
        return ResponseEntity.ok("Testing done.");
    }

    //upload file to google drive
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileToDrive(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("accessToken") String accessToken,
            @RequestParam("expiryDate")LocalDate expiryDate
            ) {
        DocumentRequestDto requestDto = DocumentRequestDto.builder()
                .title(title)
                .description(description)
                .accessToken(accessToken)
                .expiryDate(expiryDate)
                .build();
        documentService.uploadFileToGoogleDrive(file, requestDto);
        return ResponseEntity.ok("File uploaded successfully");
    }

    //get all document by user
    @GetMapping()
    public ResponseEntity<List<Document>> getDocumentsByUser(@RequestParam String accessToken) {

        List<Document> documents = documentService.getAllDocumentsByUser(accessToken);

        return ResponseEntity.ok(documents);
    }
}
