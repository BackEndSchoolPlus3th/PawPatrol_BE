package com.patrol.domain.ai.service;

import com.patrol.domain.ai.entity.AiImage;
import com.patrol.domain.ai.event.AiImageSavedEvent;
import com.patrol.domain.ai.repository.AiImageRepository;
import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.repository.CommentRepository;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiImageService {
    private final AiImageRepository aiImageRepository;
    private final LostFoundPostRepository lostFoundPostRepository;
    private final CommentRepository commentRepository;
    private final ImageRepository imageRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRepository memberRepository;

    @Value("${app.domain}")
    private String domain;


    @Transactional
    public void saveAiImages (List<MultipartFile> images, Long foundId, LostFoundPost lostFoundPost) {
        Image firstImage = null;

        if (images != null && images.size() == 1) {
            firstImage = imageRepository.findByFoundId(foundId); // 단일 이미지 조회
        } else {
            firstImage = imageRepository.findFirstByFoundIdOrderByCreatedAtAsc(foundId).orElse(null); // 가장 오래된 이미지 조회
        }

        AiImage aiImage = null;
        if (firstImage != null) {
            String firstImagePath = firstImage.getPath();

            aiImage = new AiImage();
            aiImage.setLostFoundPost(lostFoundPost);
            aiImage.setPath(firstImagePath);
            aiImage.setCreatedAt(LocalDateTime.now());
            aiImage.setStatus(lostFoundPost.getStatus());
            aiImage.setAnimalType(lostFoundPost.getAnimalType());

            aiImageRepository.save(aiImage);
            log.info("✅ AI 이미지 저장 완료! ID={}, 상태={}", aiImage.getId(), aiImage.getStatus());

        } else {
            log.warn("🚨 저장할 이미지가 없습니다. foundId={}", foundId);
        }
        eventPublisher.publishEvent(new AiImageSavedEvent(aiImage));
    }

    @Transactional
    public void linkSightedToFindingPost(AiImage newImage, AiImage targetImage, double similarity) {
        LostFoundPost targetPost = lostFoundPostRepository.findById(targetImage.getLostFoundPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("🚨 해당 이미지 ID에 대한 게시글을 찾을 수 없음: " + targetImage.getId()));
        LostFoundPost findWantPost = lostFoundPostRepository.findById(newImage.getLostFoundPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("🚨 해당 이미지 ID에 대한 게시글을 찾을 수 없음: " + newImage.getId()));

        String imageUrl = findWantPost.getImages().isEmpty() ? "이미지 없음" : findWantPost.getImages().get(0).getPath();
        String postUrl = domain + "/PetPostDetail/" + findWantPost.getId();

        String commentContent = String.format(
                "🔍 유사한 목격 제보가 있습니다!\n\n내용: %s\n🖼️ 이미지: %s\n🔗 [게시글 보기](%s)\n📝 유사도: %.2f",
                findWantPost.getContent(), imageUrl, postUrl, similarity
        );

        Comment comment = Comment.builder()
                .lostFoundPost(targetPost)
                .author(memberRepository.getMemberById(4L))
                .content(commentContent)
                .build();

        commentRepository.save(comment);

        log.info("✅ AI 덧글 연동 완료 (유사도: {:.2f})", similarity);
    }
}
