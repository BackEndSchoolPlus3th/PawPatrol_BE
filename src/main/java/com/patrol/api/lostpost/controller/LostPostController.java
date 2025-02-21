package com.patrol.api.lostpost.controller;

import com.patrol.api.lostfound.dto.FindPostResponseDto;
import com.patrol.api.lostpost.dto.LostPostRequestDto;
import com.patrol.api.lostpost.dto.LostPostResponseDto;
import com.patrol.domain.lostpost.service.LostPostService;
import com.patrol.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/lost-found/lost")
@RequiredArgsConstructor
public class LostPostController {

    private final LostPostService lostPostService;

    // 실종 신고 게시글 등록
    @PostMapping
    @Operation(summary = "실종 신고 게시글 등록")
    public RsData<LostPostResponseDto> createLostPost(
            @RequestBody LostPostRequestDto requestDto,
            @RequestParam(value = "image", required = false) MultipartFile image) {
        LostPostResponseDto responseDto = lostPostService.createLostPost(requestDto, image);
        return new RsData<>("200", "실종 신고 게시글을 성공적으로 등록했습니다.", responseDto);
    }

    // 실종 신고 게시글 수정
    @PutMapping("/{postId}")
    @Operation(summary = "실종 신고 게시글 수정")
    public RsData<LostPostResponseDto> updateLostPost(
            @PathVariable(name = "postId") Long postId,
            @RequestBody LostPostRequestDto requestDto) {
        LostPostResponseDto responseDto = lostPostService.updateLostPost(postId, requestDto);
        return new RsData<>("200", "실종 신고 게시글을 성공적으로 수정했습니다.", responseDto);
    }

    // 실종 신고 게시글 삭제
    @DeleteMapping("/{postId}")
    @Operation(summary = "실종 신고 게시글 삭제")
    public RsData<Void> deleteLostPost(@PathVariable(name = "postId") Long postId) {
        lostPostService.deleteLostPost(postId);
        return new RsData<>("200", "실종 신고 게시글을 성공적으로 삭제했습니다.");
    }

    // 모든 실종 신고 게시글 조회 (페이징 지원)
    @GetMapping
    @Operation(summary = "모든 실종 신고 게시글 조회 (페이징 지원)")
    public RsData<Page<LostPostResponseDto>> getAllLostPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LostPostResponseDto> posts = lostPostService.getAllLostPosts(pageable);
        return new RsData<>("200", "실종 신고 게시글 목록을 성공적으로 호출했습니다.", posts);
    }

    // 실종 신고 게시글 상세 조회
    @GetMapping("/{postId}")
    @Operation(summary = "실종 신고 게시글 상세 조회")
    public RsData<LostPostResponseDto> getLostPostById(@PathVariable(name = "postId") Long postId) {
        LostPostResponseDto responseDto = lostPostService.getLostPostById(postId);
        return new RsData<>("200", "실종 신고 게시글을 성공적으로 조회했습니다.", responseDto);
    }

    // 특정 실종 신고글에 대한 제보글 목록 조회
    @GetMapping("/{postId}/find-posts")
    @Operation(summary = "특정 실종 신고글에 연결된 제보글 목록 조회")
    public RsData<Page<FindPostResponseDto>> getFindPostsByLostPost(
            @PathVariable(name = "postId") Long postId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FindPostResponseDto> findPosts = lostPostService.getFindPostsByLostId(postId, pageable);
        return new RsData<>("200", "특정 실종 신고글에 대한 제보글 목록을 성공적으로 호출했습니다.", findPosts);
    }
}
