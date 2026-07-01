package com.gitscope.controller;

import com.gitscope.dto.AnalyzeRequest;
import com.gitscope.model.AnalyticsResult;
import com.gitscope.service.GitAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final GitAnalyticsService analyticsService;

    public ApiController(GitAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "service", "gitscope"));
    }

    @PostMapping("/analyze")
    public ResponseEntity<AnalyticsResult> analyze(@RequestBody(required = false) AnalyzeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required with either 'path' or 'url'.");
        }
        boolean hasPath = request.getPath() != null && !request.getPath().isBlank();
        boolean hasUrl = request.getUrl() != null && !request.getUrl().isBlank();

        if (!hasPath && !hasUrl) {
            throw new IllegalArgumentException("Provide either 'path' (a local repo directory) or 'url' (a clone URL).");
        }
        if (hasPath && hasUrl) {
            throw new IllegalArgumentException("Provide only one of 'path' or 'url', not both.");
        }

        AnalyticsResult result = hasPath
                ? analyticsService.analyzeLocal(request.getPath().trim())
                : analyticsService.analyzeRemote(request.getUrl().trim());
        return ResponseEntity.ok(result);
    }
}
