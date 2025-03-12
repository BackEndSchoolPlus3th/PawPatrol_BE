package com.patrol.domain.comment.service;

import com.patrol.api.comment.dto.CommentRequestDto;
import com.patrol.api.comment.dto.CommentResponseDto;
import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.repository.CommentRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.domain.lostFoundPost.service.NotificationService;
import com.patrol.domain.member.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final LostFoundPostRepository lostFoundPostRepository;

    @Transactional
    public CommentResponseDto createComment(CommentRequestDto requestDto, Member author) {
        Comment comment = new Comment();
        comment.setContent(requestDto.getContent());
        comment.setAuthor(author);

        if (requestDto.getLostFoundPostId() != null) {
            LostFoundPost lostFoundPost = lostFoundPostRepository.findById(requestDto.getLostFoundPostId())
                    .orElseThrow(() -> new RuntimeException("해당 ID의 제보 게시글을 찾을 수 없습니다."));
            comment.setLostFoundPost(lostFoundPost);
            notificationService.sendLostFoundPostNotification(lostFoundPost);
        }
        commentRepository.saveAndFlush(comment);
        return new CommentResponseDto(comment);
    }

    @Transactional
    public CommentResponseDto updateComment(Long commentId, CommentRequestDto requestDto,  Member author) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

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
        if (!comment.getAuthor().equals(author)) {
            throw new RuntimeException("댓글 삭제 권한이 없습니다.");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByLostFoundPost(Long lostFoundPostId) {
        List<Comment> comments = commentRepository.findByLostFoundPostId(lostFoundPostId);

        return comments.stream().map(CommentResponseDto::new).collect(Collectors.toList());
    }

}
