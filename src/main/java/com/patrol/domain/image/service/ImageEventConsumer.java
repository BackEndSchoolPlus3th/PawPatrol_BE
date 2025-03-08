package com.patrol.domain.image.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.ai.AiClient;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEventConsumer {
    private final ImageRepository imageRepository;
    private final AiClient aiClient;
    private final LostFoundPostRepository lostFoundPostRepository;
    private final ObjectMapper objectMapper;
    private final ImageService imageService;
    private final PythonMLService pythonMLService;

    @KafkaListener(topics = "image-events", groupId = "image-embedding-processor")
    @Transactional
    public void processImageEvent(@Payload String message) throws IOException {
        log.info("📩 Kafka 메시지 수신: {}", message);
        try {
            Map<String, String> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long imageId = Long.parseLong(event.get("imageId"));
            String imageUrl = event.get("imageUrl");

            log.info("🔵 Kafka 이벤트 처리 시작: imageId={}, imageUrl={}", imageId, imageUrl);

            Image image = imageRepository.findById(imageId)
                    .filter(img -> img.getStatus() != null) // status가 null이면 무시
                    .orElse(null);

            if (image == null) {
                log.info("🚨 이미지 ID {}는 status가 null이므로 Kafka 처리에서 제외됨", imageId);
                return;
            }

            log.info("🔍 AI 서버에 이미지 분석 요청: imageId={}", imageId);
            Map<String, String> embeddingData = aiClient.extractEmbeddingAndFeaturesFromUrl(imageUrl);

            if (!embeddingData.containsKey("embedding")) {
                throw new RuntimeException("🚨 임베딩 추출 실패: imageId=" + imageId);
            }

            image.setEmbedding(embeddingData.get("embedding"));
            image.setFeatures(embeddingData.get("features"));
            imageRepository.save(image);

            log.info("✅ 임베딩 저장 완료: imageId={}", imageId);

            // ✅ 이미지가 FINDING이면 기존 방식 유지
            if (image.getStatus() == PostStatus.FINDING) {
                processImageFindEvent(message);
            }
            // ✅ 이미지가 SIGHTED이면 새 로직 실행
            else if (image.getStatus() == PostStatus.SIGHTED) {
                processSightedImageEvent(message);
            }

        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        }
    }


    /**
     * 🔵 2️⃣ 유사도 분석 실행 (임베딩 추출 후 실행)
     */
    @Transactional
    public void processImageFindEvent(@Payload String message) {
        log.info("📩 Kafka 메시지 수신 (유사도 분석): {}", message);
        try {
            Map<String, String> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long imageId = Long.parseLong(event.get("imageId"));

            log.info("🔵 Kafka 이벤트 처리 시작 (유사도 분석): imageId={}", imageId);

            // ✅ FINDING 게시글 기준으로 반경 5km 내 SIGHTED 게시글 찾기
            Image findingImage = imageRepository.findById(imageId).filter(img -> img.getStatus() == PostStatus.FINDING)
                    .orElseThrow(() -> new RuntimeException("이미지 ID 실종글" + imageId + "를 찾을 수 없음"));

            LostFoundPost findingPost = lostFoundPostRepository.findById(findingImage.getFoundId())
                    .orElseThrow(() -> new RuntimeException("FINDING 게시글을 찾을 수 없음"));

            // ✅ 반경 5km 내에서 'sighted' 게시글 조회
            List<LostFoundPost> sightedPosts = lostFoundPostRepository.findSightedPostsWithinRadius(
                    findingPost.getLatitude(), findingPost.getLongitude(), 5.0, findingPost.getAnimalType().name()
            );


            for (LostFoundPost sightedPost : sightedPosts) {
                Image sightedImage = imageRepository.findByFoundId(sightedPost.getId());
                if (sightedImage == null || sightedImage.getStatus() == null) continue;

                double similarity = pythonMLService.compareEmbeddingsAndFeatures(
                        extractEmbeddingAsList(findingImage.getEmbedding()),
                        extractEmbeddingAsList(findingImage.getFeatures()),
                        extractEmbeddingAsList(sightedImage.getEmbedding()),
                        extractEmbeddingAsList(sightedImage.getFeatures())
                );


                if (similarity >= 0.85) {
                    log.info("🔍 유사한 게시글 발견! sightedId={}, 유사도={}", sightedPost.getId(), similarity);
                    imageService.linkSightedToFindingPost(findingImage, sightedImage, similarity);
                }
            }

        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processSightedImageEvent(@Payload String message) {
        log.info("📩 Kafka 메시지 수신 (유사도 분석 - SIGHTED 기준): {}", message);
        try {
            Map<String, String> event = objectMapper.readValue(message, new TypeReference<>() {});
            Long imageId = Long.parseLong(event.get("imageId"));

            log.info("🔵 Kafka 이벤트 처리 시작 (유사도 분석 - SIGHTED 기준): imageId={}", imageId);

            Image sightedImage = imageRepository.findById(imageId)
                    .filter(img -> img.getStatus() == PostStatus.SIGHTED)
                    .orElseThrow(() -> new RuntimeException("이미지 ID 제보글 " + imageId + "를 찾을 수 없음"));

            LostFoundPost sightedPost = lostFoundPostRepository.findById(sightedImage.getFoundId())
                    .orElseThrow(() -> new RuntimeException("SIGHTED 게시글을 찾을 수 없음"));

            List<LostFoundPost> findingPosts = lostFoundPostRepository.findFindingPostsWithinRadius(
                    sightedPost.getLatitude(), sightedPost.getLongitude(), 5.0, sightedPost.getAnimalType().name()
            );

            for (LostFoundPost findingPost : findingPosts) {
                Image findingImage = imageRepository.findByFoundId(findingPost.getId());
                if (findingImage == null || findingImage.getStatus() == null) continue;

                double similarity = pythonMLService.compareEmbeddingsAndFeatures(
                        extractEmbeddingAsList(sightedImage.getEmbedding()),
                        extractEmbeddingAsList(sightedImage.getFeatures()),
                        extractEmbeddingAsList(findingImage.getEmbedding()),
                        extractEmbeddingAsList(findingImage.getFeatures())
                );

                if (similarity >= 0.85) {
                    log.info("🔍 유사한 실종글 발견! findingId={}, 유사도={}", findingPost.getId(), similarity);
                    imageService.linkSightedToFindingPost(findingImage, sightedImage, similarity);
                }
            }

        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    /**
     * 🔵 4️⃣ JSON 문자열 임베딩 데이터를 List<Double> 변환
     */
    private List<Double> extractEmbeddingAsList(String jsonEmbedding) {
        try {
            return jsonEmbedding != null ? objectMapper.readValue(jsonEmbedding, new TypeReference<>() {}) : Collections.emptyList();
        } catch (Exception e) {
            log.error("🚨 임베딩 변환 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
