package com.patrol.api.comment.controller;

import com.patrol.api.comment.dto.CommentRequestDto;
import com.patrol.api.comment.dto.CommentResponseDto;
import com.patrol.domain.comment.service.CommentService;
import com.patrol.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "댓글 등록")
    public RsData<CommentResponseDto> createComment(@RequestBody CommentRequestDto requestDto) {
        CommentResponseDto responseDto = commentService.createComment(requestDto);
        return new RsData<>("200", "댓글이 등록되었습니다.", responseDto);
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "댓글 수정")
    public RsData<CommentResponseDto> updateComment(@PathVariable(name = "commentId") Long commentId, @RequestBody CommentRequestDto requestDto) {
        CommentResponseDto responseDto = commentService.updateComment(commentId, requestDto);
        return new RsData<>("200", "댓글이 수정되었습니다.", responseDto);
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제")
    public RsData<Void> deleteComment(@PathVariable(name = "commentId")  Long commentId) {
        commentService.deleteComment(commentId);
        return new RsData<>("200", "댓글이 삭제되었습니다.");
    }

    @GetMapping("/lost-post/{lostPostId}")
    @Operation(summary = "신고글에 대한 댓글 조회")
    public RsData<List<CommentResponseDto>> getCommentsByLostPost(@PathVariable(name = "lostPostId") Long lostPostId) {
        List<CommentResponseDto> comments = commentService.getCommentsByLostPost(lostPostId);
        return new RsData<>("200", "댓글 목록 조회 성공", comments);
    }

    @GetMapping("/find-post/{findPostId}")
    @Operation(summary = "제보글에 대한 댓글 조회")
    public RsData<List<CommentResponseDto>> getCommentsByFindPost(@PathVariable(name = "findPostId") Long findPostId) {
        List<CommentResponseDto> comments = commentService.getCommentsByFindPost(findPostId);
        return new RsData<>("200", "댓글 목록 조회 성공", comments);
    }
}

