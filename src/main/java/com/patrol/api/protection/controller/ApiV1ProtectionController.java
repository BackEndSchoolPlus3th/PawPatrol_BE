package com.patrol.api.protection.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.animalCase.dto.AnimalCaseListResponse;
import com.patrol.api.protection.dto.*;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.protection.service.ProtectionService;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/protections")
@Tag(name = "임시보호/입양 관리 API", description = "임시보호/입양 목록에서 신청, 조회 등")
public class ApiV1ProtectionController {

  private final ProtectionService protectionService;
  private final ObjectMapper objectMapper;

  @GetMapping
  @Operation(summary = "임시보호/입양 대기 중인 동물 목록")
  public RsData<Page<AnimalCaseListResponse>> getPossibleProtections(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) AnimalType animalType,
      @RequestParam(required = false) String location
  ) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<AnimalCaseListResponse> response = protectionService.findPossibleAnimalCases(
        pageable, animalType, location);
    return new RsData<>("200", "보호 희망 동물의 목록 조회 성공", response);
  }


  @GetMapping("/{caseId}")
  @Operation(summary = "임시보호/입양 대기 중인 동물 상세 조회")
  public RsData<AnimalCaseDetailResponse> getPossibleAnimalCase(
      @PathVariable Long caseId, @LoginUser Member loginUser
  ) {
    AnimalCaseDetailResponse response = protectionService.findPossibleAnimalCase(caseId, loginUser.getId());
    return new RsData<>("200", "보호 희망 동물의 상세 조회 성공", response);
  }


  @PostMapping
  @Operation(summary = "임시보호/입양 동물 등록하기")
  public RsData<Void> createAnimalCase(
      @RequestParam("metadata") String metadataJson,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      @LoginUser Member loginUser
  ) {
    try {
      CreateAnimalCaseRequest request = objectMapper.readValue(metadataJson, CreateAnimalCaseRequest.class);
      protectionService.createAnimalCase(request, loginUser, images);
      return new RsData<>("200", "보호 희망 동물의 등록 성공");
    } catch (JsonProcessingException e) {
      System.err.println("JSON 파싱 오류: " + e.getMessage());
      e.printStackTrace();
      return new RsData<>("400", "잘못된 JSON 형식입니다.");
    }
  }


  @PutMapping("/{caseId}")
  @Operation(summary = "임시보호/입양 동물 정보 수정")
  public RsData<Void> updateAnimalCase(
      @PathVariable Long caseId,
      @RequestParam("metadata") String metadataJson,
      @RequestParam(value = "images", required = false) List<MultipartFile> images,
      @LoginUser Member loginUser
  ) {
    try {
      UpdateAnimalCaseRequest request = objectMapper.readValue(metadataJson, UpdateAnimalCaseRequest.class);
      protectionService.updateAnimalCase(caseId, request, loginUser, images);
      return new RsData<>("200", "보호 희망 동물의 정보 수정 성공");
    } catch (JsonProcessingException e) {
      return new RsData<>("400", "잘못된 JSON 형식입니다.");
    }
  }


  @PatchMapping("/{caseId}")
  @Operation(summary = "임시보호/입양 대기 중인 동물 소프트 삭제")
  public RsData<Void> deleteAnimalCase(
      @PathVariable Long caseId, @LoginUser Member loginUser
  ) {
    protectionService.deleteAnimalCase(caseId, loginUser.getId());
    return new RsData<>("200", "보호 희망 동물의 삭제 성공");
  }


  @GetMapping("/my-cases")
  @Operation(summary = "내가 등록한 임시보호/입양 동물 목록")
  public RsData<MyAnimalCasePageResponse> getMyAnimalCases(
      @LoginUser Member loginUser,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    MyAnimalCasePageResponse response = protectionService.findMyAnimalCases(loginUser, pageable);
    return new RsData<>("200", "내가 등록한 보호 희망 동물의 목록 조회 성공", response);
  }


  @GetMapping("/my-protections")
  @Operation(summary = "내가 신청한 임시보호/입양 신청 목록")
  public RsData<Page<ProtectionResponse>> getMyProtections(
      @LoginUser Member loginUser,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Page<ProtectionResponse> response = protectionService.findMyProtections(loginUser.getId(), pageable);
    return new RsData<>("200", "내가 신청한 임시보호/입양 신청 목록 조회 성공", response);
  }


  @PostMapping("/{caseId}/apply")
  @Operation(summary = "임시보호/입양 신청하기")
  public RsData<ProtectionResponse> applyProtection(
      @PathVariable Long caseId, @RequestBody ProtectionRequest request, @LoginUser Member loginUser
  ) {
    ProtectionResponse
        response = protectionService.applyProtection(caseId, loginUser.getId(), request.reason(), request.protectionType());
    return new RsData<>("200", "임시보호/입양 신청하기 성공", response);
  }


  @PatchMapping("/{protectionId}/cancel")
  @Operation(summary = "임시보호/입양 신청 취소")
  public RsData<Void> cancelProtection(
      @PathVariable Long protectionId, @LoginUser Member loginUser
  ) {
    protectionService.cancelProtection(protectionId, loginUser.getId());
    return new RsData<>("200", "임시보호/입양 신청 취소 완료");
  }


  @PatchMapping("/{protectionId}/accept")
  @Operation(summary = "임시보호/입양 신청 수락")
  public RsData<Void> acceptProtection(
      @PathVariable Long protectionId, @LoginUser Member loginUser
  ) {
    protectionService.acceptProtection(protectionId, loginUser.getId());
    return new RsData<>("200", "임시보호/입양 신청 수락 완료");
  }


  @PatchMapping("/{protectionId}/reject")
  @Operation(summary = "임시보호/입양 신청 거절")
  public RsData<Void> rejectProtection(
      @PathVariable Long protectionId, @LoginUser Member loginUser,
      @RequestBody RejectApplicationRequest request
  ) {
    protectionService.rejectProtection(protectionId, loginUser.getId(), request.rejectReason());
    return new RsData<>("200", "임시보호/입양 신청 거절 완료");
  }
}
