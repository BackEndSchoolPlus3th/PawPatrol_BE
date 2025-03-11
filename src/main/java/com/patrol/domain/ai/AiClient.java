package com.patrol.domain.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service-url}")
    private String aiServiceUrl;

    public Map<String, String> extractEmbeddingAndFeaturesFromUrl(String imageUrl) throws IOException {
        log.info("🔍 AI 서비스 임베딩 추출 시작: {}", imageUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("image_url", imageUrl);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

        try {
            String endpoint = aiServiceUrl + "/extract-embedding-from-url";
            log.info("📡 AI 서비스 요청: POST {}", endpoint);

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                if (jsonNode == null || jsonNode.get("embedding") == null || jsonNode.get("features") == null) {
                    log.error("🚨 FastAPI 임베딩 추출 실패: 응답 값이 유효하지 않음");
                    return Map.of("embedding", "", "features", "");
                }

                return Map.of(
                        "embedding", jsonNode.get("embedding").toString(),
                        "features", jsonNode.get("features").toString()
                );
            } else {
                log.error("❌ AI 서비스 응답 오류: {}", response.getStatusCode());
                throw new IOException("URL 임베딩 추출 API 호출 실패: " + response.getStatusCode());
            }
        } catch (RestClientException e) {
            log.error("❌ AI 서비스 통신 오류: {}", e.getMessage(), e);
            throw new IOException("AI 서비스 연결 오류: " + e.getMessage(), e);
        }
    }

    public double calculateSimilarity(
            List<Double> findingEmbedding, List<Double> findingFeatures,
            List<Double> sightedEmbedding, List<Double> sightedFeatures) {
        log.info("🔍 FastAPI 유사도 비교 요청");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("finding_embedding", findingEmbedding);
        requestBody.put("finding_features", findingFeatures);
        requestBody.put("sighted_embedding", sightedEmbedding);
        requestBody.put("sighted_features", sightedFeatures);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            String endpoint = aiServiceUrl + "/compare-embeddings";
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(endpoint, requestEntity, JsonNode.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().get("similarity").asDouble();
            }
        } catch (Exception e) {
            log.error("❌ FastAPI 유사도 비교 요청 실패: {}", e.getMessage());
        }
        return 0.0;
    }
}
