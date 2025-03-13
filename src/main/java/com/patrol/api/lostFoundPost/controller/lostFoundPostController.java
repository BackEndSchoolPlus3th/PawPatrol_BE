package com.patrol.api.lostFoundPost.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.lostFoundPost.dto.LostFoundPostDetailResponseDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostRequestDto;
import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.domain.lostFoundPost.entity.PostStatus;
import com.patrol.domain.lostFoundPost.service.LostFoundPostService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.rsData.RsData;
import com.patrol.global.webMvc.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lost-foundposts")
@RequiredArgsConstructor
@Tag(name = "제보 게시글 API", description = "제보글")
public class lostFoundPostController {

    private final LostFoundPostService lostFoundPostService;
    private final ObjectMapper objectMapper;

    @PostMapping
    @Operation(summary = "제보 게시글 등록") //해결
    public RsData<LostFoundPostResponseDto> createStandaloneFindPost(
            @RequestParam("metadata") String metadataJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "petId", required = false) Long petId,
            @LoginUser Member loginUser) {
        try {
            LostFoundPostRequestDto requestDto = objectMapper.readValue(metadataJson, LostFoundPostRequestDto.class);
            // 이미지가 null일 경우 빈 리스트로 초기화
            if (images == null) {
                images = new ArrayList<>();
            }
            LostFoundPostResponseDto responseDto = lostFoundPostService.createLostFoundPost(requestDto, petId, loginUser, images);
            return new RsData<>("200", "제보 게시글을 성공적으로 등록했습니다.", responseDto);
        } catch (JsonProcessingException e) {
            return new RsData<>("400", "잘못된 JSON 형식입니다.", null);
        }
    }



    @PutMapping("/{postId}")
    @Operation(summary = "제보 게시글 수정")
    public RsData<LostFoundPostResponseDto> updateStandaloneFindPost(
            @PathVariable(name = "postId") Long postId,
            @RequestParam("metadata") String metadataJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @LoginUser Member loginUser) {
        try {
            LostFoundPostResponseDto requestDto = objectMapper.readValue(metadataJson, LostFoundPostResponseDto.class);
            LostFoundPostResponseDto responseDto = lostFoundPostService.updateLostFoundPost(postId, requestDto, images,loginUser);
            return new RsData<>("200", "제보 게시글을 성공적으로 수정했습니다.", responseDto);
        } catch (JsonProcessingException e) {
            return new RsData<>("400", "잘못된 JSON 형식입니다.", null);
        }
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "제보 게시글 삭제")
    public RsData<Void> deleteFindPost(@PathVariable(name = "postId") Long postId,@LoginUser Member loginUser) {
        lostFoundPostService.deleteLostFoundPost(postId,loginUser);
        return new RsData<>("200", "제보 게시글을 성공적으로 삭제했습니다.");
    }

    @GetMapping("/map")
    @Operation(summary = "반경 내의 모든 신고글 연계 제보 게시글 조회 (페이징 지원)")
    public RsData<List<LostFoundPostResponseDto>> getAllFindPosts(
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude,
            @RequestParam(name = "radius") double radius) {

        List<LostFoundPostResponseDto> posts = lostFoundPostService.getLostFoundPostsWithinRadius(latitude, longitude, radius);
        return new RsData<>("200", "반경 내의 제보 게시글을 성공적으로 호출했습니다.", posts);
    }

    @GetMapping
    @Operation(summary = "제보 게시글 목록 조회")
    public RsData<Page<LostFoundPostResponseDto>> getAllFindPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LostFoundPostResponseDto> posts = lostFoundPostService.getAllLostFoundPosts(pageable);
        return new RsData<>("200", "제보 게시글 목록을 성공적으로 호출했습니다.", posts);
    }

    @GetMapping("/{postId}")
    @Operation(summary = "제보 게시글 상세 조회")
    public RsData<LostFoundPostDetailResponseDto> getFindPostById(@PathVariable(name = "postId") Long postId) {
        LostFoundPostDetailResponseDto responseDto = lostFoundPostService.getLostFoundPostById(postId);
        return new RsData<>("200", "제보 게시글을 성공적으로 조회했습니다.", responseDto);
    }


    @GetMapping("/finding")
    @Operation(summary = "실종 게시글 목록 조회")
    public RsData<Page<LostFoundPostResponseDto>> getAllFindingPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LostFoundPostResponseDto> posts = lostFoundPostService.getPostsByStatus(PostStatus.FINDING, pageable);
        return new RsData<>("200", "실종 게시글 목록을 성공적으로 호출했습니다.", posts);
    }

    @GetMapping("/sighted")
    @Operation(summary = "목격 게시글 목록 조회")
    public RsData<Page<LostFoundPostResponseDto>> getAllSightedPosts(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LostFoundPostResponseDto> posts = lostFoundPostService.getPostsByStatus(PostStatus.SIGHTED, pageable);
        return new RsData<>("200", "목격 게시글 목록을 성공적으로 호출했습니다.", posts);
    }

    @GetMapping("reward-list")
    @Operation(summary = "보상금이 있는 실종 게시글 목록 조회")
    public RsData<Page<LostFoundPostResponseDto>> getAllRewardFindingPosts(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<LostFoundPostResponseDto> posts = lostFoundPostService.getRewardPosts(PostStatus.FINDING, pageable);
        return new RsData<>("200", "실종 게시글 목록 목록을 성공적으로 호출했습니다.", posts);
    }
}
