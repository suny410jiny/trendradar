package com.trendradar.youtube.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ClaudeApiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public ClaudeApiClient(
            @Value("${claude.api.key:}") String apiKey,
            @Value("${claude.api.base-url:https://api.anthropic.com}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public String generateBriefing(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Claude API key not configured, returning default message");
            return "AI 브리핑 서비스 준비 중입니다.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model", "claude-sonnet-4-20250514",
                "max_tokens", 500,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/v1/messages", request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("content")) {
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
                if (!content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }
            return "브리핑 생성에 실패했습니다.";
        } catch (Exception e) {
            log.error("Claude API call failed", e);
            throw new RuntimeException("AI 브리핑 생성 중 오류가 발생했습니다.", e);
        }
    }
}
