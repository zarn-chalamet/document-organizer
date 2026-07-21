package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.DashboardSummaryDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return ResponseEntity.ok(dashboardService.getSummary(user.getEmail()));
    }
}