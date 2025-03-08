package com.patrol.domain.comment.service;

import com.patrol.api.comment.dto.CommentRequestDto;
import com.patrol.api.comment.dto.CommentResponseDto;
import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.repository.CommentRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.domain.lostFoundPost.service.NotificationService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final LostFoundPostRepository lostFoundPostRepository;

    private final NotificationService notificationService; // WebSocket service to send notificationss

    @Transactional
    public CommentResponseDto createComment(CommentRequestDto requestDto, Member author) {
        Comment comment = new Comment();
        comment.setContent(requestDto.getContent());

        comment.setAuthor(author);

        // FindPost 조회 후 설정
        if (requestDto.getLostFoundPostId() != null) {
            LostFoundPost lostFoundPost = lostFoundPostRepository.findById(requestDto.getLostFoundPostId())
                    .orElseThrow(() -> new RuntimeException("해당 ID의 제보 게시글을 찾을 수 없습니다."));
            comment.setLostFoundPost(lostFoundPost);

            // After saving the comment, send a notification via WebSocket
            notificationService.sendLostFoundPostNotification(lostFoundPost);
        }

        // 저장 후 강제 플러시
        commentRepository.saveAndFlush(comment);
        return new CommentResponseDto(comment);
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto,  Member author) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        // 로그인한 사용자(author)가 댓글 작성자와 일치하는지 확인
        if (!comment.getAuthor().equals(author)) {
            throw new RuntimeException("댓글 수정 권한이 없습니다.");
        }
        comment.setContent(requestDto.getContent());
        return new CommentResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Member author) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        // 로그인한 사용자(author)가 댓글 작성자와 일치하는지 확인
        if (!comment.getAuthor().equals(author)) {
            throw new RuntimeException("댓글 삭제 권한이 없습니다.");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByLostFoundPost(Long lostFoundPostId) {
        List<Comment> comments = commentRepository.findByLostFoundPostId(lostFoundPostId);
        if (comments.isEmpty()) {
            throw new RuntimeException("해당 제보글에 대한 댓글이 없습니다.");
        }
        return comments.stream().map(CommentResponseDto::new).collect(Collectors.toList());
    }

}
