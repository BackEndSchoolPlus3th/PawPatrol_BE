package com.patrol.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.service-url}")
    private String aiServiceUrl;

    private static final int MAX_RETRY = 3;
    private static final long RETRY_DELAY_MS = 2000;


    @Async
    public CompletableFuture<Map<String, String>> extractEmbeddingAsync(String imageUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractEmbeddingAndFeaturesFromUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public Map<String, String> extractEmbeddingAndFeaturesFromUrl(String imageUrl) throws IOException {
        log.info("🔍 AI 서비스 임베딩 추출 시작: {}", imageUrl);

        if (!isValidImageUrl(imageUrl)) {
            log.error("❌ 유효하지 않은 이미지 URL: {}", imageUrl);
            return Map.of("embedding", "[]", "features", "[]");
        }

        String processedUrl = preprocessAndCreateTempUrl(imageUrl);
        if (processedUrl != null) {
            log.info("🔄 전처리된 이미지 URL 사용: {}", processedUrl);
            imageUrl = processedUrl;
        }

        return sendUrlRequestWithRetry(imageUrl, 0);
    }

    private boolean isValidImageUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            String contentType = connection.getContentType();

            connection.disconnect();

            boolean isValid = (responseCode == HttpURLConnection.HTTP_OK &&
                    contentType != null &&
                    contentType.startsWith("image/"));

            log.info("🔍 이미지 URL 유효성: {} (응답코드: {}, 콘텐츠타입: {})",
                    isValid, responseCode, contentType);

            return isValid;
        } catch (Exception e) {
            log.error("❌ 이미지 URL 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    private String preprocessAndCreateTempUrl(String imageUrl) {
        try {
            // 1. 이미지 다운로드
            log.info("📥 이미지 다운로드 시작: {}", imageUrl);
            URL url = new URL(imageUrl);
            BufferedImage originalImage = ImageIO.read(url);

            if (originalImage == null) {
                log.error("❌ 이미지를 읽을 수 없음: {}", imageUrl);
                return null;
            }

            log.info("✅ 이미지 다운로드 성공: {}x{}, 타입: {}",
                    originalImage.getWidth(), originalImage.getHeight(),
                    originalImage.getType());

            BufferedImage processedImage = convertToRGB(originalImage);

            return null;
        } catch (Exception e) {
            log.error("❌ 이미지 전처리 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    private BufferedImage convertToRGB(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            return original;
        }

        BufferedImage convertedImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR); // 8bit RGB 형식

        Graphics2D g2d = convertedImage.createGraphics();
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        log.info("✅ 이미지 형식 변환 완료: TYPE_3BYTE_BGR (8bit RGB)");
        return convertedImage;
    }

    private Map<String, String> sendUrlRequestWithRetry(String imageUrl, int attempt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("image_url", imageUrl);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            String endpoint = aiServiceUrl + "/extract-embedding-from-url";
            log.info("📡 AI 서비스 요청 ({}/{}): POST {}", attempt + 1, MAX_RETRY, endpoint);
            log.info("📦 요청 데이터: {}", objectMapper.writeValueAsString(body));

            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, requestEntity, String.class);

            log.info("📄 AI 서비스 응답 상태: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                if (jsonNode == null || jsonNode.get("embedding") == null || jsonNode.get("features") == null) {
                    log.error("🚨 FastAPI 임베딩 추출 실패: 응답 값이 유효하지 않음");
                    return Map.of("embedding", "[]", "features", "[]");
                }

                Map<String, String> result = new HashMap<>();
                result.put("embedding", jsonNode.get("embedding").toString());
                result.put("features", jsonNode.get("features").toString());
                return result;
            } else {
                log.error("❌ AI 서비스 응답 오류: {}", response.getStatusCode());

                if (attempt < MAX_RETRY - 1) {
                    log.info("⏱️ 재시도 대기 중... ({}/{})", attempt + 1, MAX_RETRY);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    return sendUrlRequestWithRetry(imageUrl, attempt + 1);
                }

                return Map.of("embedding", "[]", "features", "[]");
            }
        } catch (RestClientException e) {
            log.error("❌ AI 서비스 통신 오류: {}", e.getMessage());

            if (attempt < MAX_RETRY - 1) {
                try {
                    log.info("⏱️ 통신 오류로 인한 재시도 대기 중... ({}/{})", attempt + 1, MAX_RETRY);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    return sendUrlRequestWithRetry(imageUrl, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            return Map.of("embedding", "[]", "features", "[]");
        } catch (Exception e) {
            log.error("❌ 예상치 못한 오류 발생: {}", e.getMessage(), e);

            if (attempt < MAX_RETRY - 1) {
                try {
                    log.info("⏱️ 오류로 인한 재시도 대기 중... ({}/{})", attempt + 1, MAX_RETRY);
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    return sendUrlRequestWithRetry(imageUrl, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            return Map.of("embedding", "[]", "features", "[]");
        }
    }

    public double calculateSimilarity(
            List<Double> findingEmbedding, List<Double> findingFeatures,
            List<Double> sightedEmbedding, List<Double> sightedFeatures) {
        if (findingEmbedding == null || findingEmbedding.isEmpty() ||
                sightedEmbedding == null || sightedEmbedding.isEmpty()) {
            log.warn("⚠️ 빈 임베딩으로 유사도 계산 불가");
            return 0.0;
        }

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
                double similarity = response.getBody().get("similarity").asDouble();
                log.info("✅ 유사도 계산 완료: {}", similarity);
                return similarity;
            }
        } catch (Exception e) {
            log.error("❌ FastAPI 유사도 비교 요청 실패: {}", e.getMessage());
        }
        return 0.0;
    }
}
