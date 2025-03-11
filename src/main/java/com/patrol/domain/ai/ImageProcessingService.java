package com.patrol.domain.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageProcessingService {
    private final AiImageRepository aiImageRepository;
    private final PythonMLService pythonMLService;
    private final ObjectMapper objectMapper;
    private final AiImageService aiImageService;

    @Async
    public void asyncProcessImageFind(Long imageId) {
        processImageSimilarity(imageId, PostStatus.FINDING);
    }

    @Async
    public void asyncProcessSightedImage(Long imageId) {
        processImageSimilarity(imageId, PostStatus.SIGHTED);
    }

    @Transactional
    public void processImageSimilarity(Long imageId, PostStatus targetStatus) {
        try {
            log.info("📩 Kafka 메시지 수신 (유사도 분석 - {} 기준): imageId={}", targetStatus, imageId);

            // 1️⃣ 분석할 새 이미지 조회
            AiImage newImage = aiImageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("🚨 이미지 ID " + imageId + "를 찾을 수 없음"));

            if (newImage.getEmbedding() == null) {
                log.warn("🚨 임베딩이 존재하지 않는 이미지입니다: imageId={}", imageId);
                return; // 임베딩이 없는 경우 비교하지 않음
            }

            PostStatus oppositeStatus = (targetStatus == PostStatus.FINDING) ? PostStatus.SIGHTED : PostStatus.FINDING;

            List<AiImage> nearbyTargetImages = aiImageRepository.findNearbyAiImages(
                            newImage.getLostFoundPost().getLatitude(),
                            newImage.getLostFoundPost().getLongitude(),
                            10.0 // 반경 10km 제한
                    ).stream()
                    .filter(img -> img.getStatus() == oppositeStatus)  // 반대되는 상태 필터링
                    .toList();


            // ✅ 임베딩이 완료된 이미지만 필터링
            nearbyTargetImages = nearbyTargetImages.stream()
                    .filter(img -> img.getEmbedding() != null)
                    .toList();

            log.info("🔍 유사도 분석 대상: {}개", nearbyTargetImages.size());

            for (AiImage targetImage : nearbyTargetImages) {
                double similarity = pythonMLService.compareEmbeddingsAndFeatures(
                        extractEmbeddingAsList(newImage.getEmbedding()),
                        extractEmbeddingAsList(newImage.getFeatures()),
                        extractEmbeddingAsList(targetImage.getEmbedding()),
                        extractEmbeddingAsList(targetImage.getFeatures())
                );

                if (similarity >= 0.85) {
                    log.info("🔍 유사한 게시글 발견! targetId={}, 유사도={}", targetImage.getId(), similarity);
                    aiImageService.linkSightedToFindingPost(newImage, targetImage, similarity);
                }
            }
        } catch (Exception e) {
            log.error("🚨 Kafka 메시지 처리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    private List<Double> extractEmbeddingAsList(String jsonEmbedding) {
        try {
            return jsonEmbedding != null ? objectMapper.readValue(jsonEmbedding, new TypeReference<>() {}) : Collections.emptyList();
        } catch (Exception e) {
            log.error("🚨 임베딩 변환 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
