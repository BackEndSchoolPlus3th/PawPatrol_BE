package com.patrol.domain.ai;

import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.repository.CommentRepository;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class AiImageService {
    private final AiImageRepository aiImageRepository;
    private final ImageEventProducer imageEventProducer;
    private final LostFoundPostRepository lostFoundPostRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void processImageEmbedding(AiImage image) {
        boolean isEmbedded = aiImageRepository.existsByPath(image.getPath());

        if (!isEmbedded) {
            log.info("📌 임베딩되지 않은 이미지 발견. Kafka 이벤트 전송: {}", image.getPath());
            imageEventProducer.sendImageEvent(image.getId(), image.getPath());
        } else {
            log.info("✅ 해당 이미지는 이미 임베딩됨: {}", image.getId());
        }
    }

    @Transactional
    public void linkSightedToFindingPost(AiImage newImage, AiImage targetImage, double similarity) {

        LostFoundPost targetPost = lostFoundPostRepository.findById(targetImage.getLostFoundPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("🚨 해당 이미지 ID에 대한 게시글을 찾을 수 없음: " + targetImage.getId()));
        LostFoundPost findWantPost = lostFoundPostRepository.findById(newImage.getLostFoundPost().getId())
                .orElseThrow(() -> new IllegalArgumentException("🚨 해당 이미지 ID에 대한 게시글을 찾을 수 없음: " + newImage.getId()));
        String imageUrl = findWantPost.getImages().isEmpty() ? "이미지 없음" : findWantPost.getImages().get(0).getPath();
        String commentContent = String.format(
                "🔍 유사한 목격 제보가 있습니다!\n내용: %s\n🔗 [이미지 보기](%s)\n📝 유사도: %.2f",
                findWantPost.getContent(), imageUrl, similarity
        );

        // 3️⃣ AI 알림 사용자 설정 (시스템 계정 or NULL)
        Comment comment = Comment.builder()
                .lostFoundPost(targetPost)
                .author(null)  // 🔹 추후 AI 시스템 계정으로 변경 가능
                .content(commentContent)
                .build();

        commentRepository.save(comment);

        log.info("✅ai 덧글 연동 완료 (유사도: {:.2f})", similarity);
    }
}
