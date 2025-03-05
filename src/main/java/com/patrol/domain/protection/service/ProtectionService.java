package com.patrol.domain.protection.service;


import com.patrol.api.animalCase.dto.AnimalCaseDetailDto;
import com.patrol.api.animalCase.dto.AnimalCaseListResponse;
import com.patrol.api.protection.dto.*;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.animalCase.service.AnimalCaseEventPublisher;
import com.patrol.domain.animalCase.service.AnimalCaseService;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.service.ImageService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.service.MemberService;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.domain.protection.enums.ProtectionStatus;
import com.patrol.domain.protection.enums.ProtectionType;
import com.patrol.domain.protection.repository.ProtectionRepository;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import com.patrol.global.storage.FileUploadRequest;
import com.patrol.global.storage.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProtectionService {

  private final ProtectionRepository protectionRepository;
  private final AnimalCaseService animalCaseService;
  private final MemberService memberService;
  private final AnimalCaseEventPublisher animalCaseEventPublisher;
  private final AnimalRepository animalRepository;
  private final ImageService imageService;



  public AnimalCaseDetailResponse findPossibleAnimalCase(Long caseId, Long memberId) {
    Collection<CaseStatus> possibleStatuses = List.of(
        CaseStatus.PROTECT_WAITING,
        CaseStatus.TEMP_PROTECTING,
        CaseStatus.SHELTER_PROTECTING,
        CaseStatus.MY_PET
    );
    AnimalCase animalCase = animalCaseService.findByIdAndStatusesWithHistories(caseId, possibleStatuses)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));
    boolean isOwner = animalCase.getCurrentFoster() != null &&
        animalCase.getCurrentFoster().getId().equals(memberId);

    List<Image> images = imageService.findAllByAnimalId(animalCase.getAnimal().getId());

    if (isOwner) {
      return AnimalCaseDetailResponse.create(
          AnimalCaseDetailDto.of(animalCase), isOwner, getPendingProtections(animalCase.getId()), images
      );
    } else {
      return AnimalCaseDetailResponse.create(
          AnimalCaseDetailDto.of(animalCase), isOwner, null, images
      );
    }
  }

  public Page<AnimalCaseListResponse> findPossibleAnimalCases(Pageable pageable) {
    return animalCaseService.findAllByStatuses(
        List.of(
            CaseStatus.PROTECT_WAITING,
            CaseStatus.TEMP_PROTECTING,
            CaseStatus.SHELTER_PROTECTING
        ),
        pageable
    );
  }

  public Page<ProtectionResponse> findMyProtections(Long memberId, Pageable pageable) {
    return protectionRepository.findAllByApplicantIdAndDeletedAtIsNull(memberId, pageable)
        .map(ProtectionResponse::of);
  }

  public Optional<Protection> findById(Long protectionId) {
    return protectionRepository.findByIdWithFetchAll(protectionId);
  }


  public Page<MyAnimalCaseResponse> findMyAnimalCases(Member currentFoster, Pageable pageable) {
    Page<AnimalCase> cases = animalCaseService.findAllByCurrentFosterAndStatus(
        currentFoster, List.of(
            CaseStatus.PROTECT_WAITING,
            CaseStatus.TEMP_PROTECTING,
            CaseStatus.SHELTER_PROTECTING
        ), pageable
    );

    return cases.map(animalCase -> {
      List<PendingProtectionResponse> pendingProtections = getPendingProtections(animalCase.getId());

      int pendingCount = protectionRepository.countByAnimalCaseIdAndProtectionStatusAndDeletedAtIsNull(
          animalCase.getId(), ProtectionStatus.PENDING);
      return MyAnimalCaseResponse.of(animalCase, pendingCount, pendingProtections);
    });
  }


  @Transactional
  public ProtectionResponse applyProtection(
      Long caseId, Long memberId, String reason, ProtectionType protectionType
  ) {
    Member applicant = memberService.findById(memberId)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    AnimalCase animalCase = animalCaseService.findByIdAndStatusesWithHistories(caseId,
            List.of(CaseStatus.PROTECT_WAITING, CaseStatus.TEMP_PROTECTING, CaseStatus.SHELTER_PROTECTING))
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    if (animalCase.getCurrentFoster() == null) {
      throw new CustomException(ErrorCode.NOT_ASSIGNED_PROTECTION);
    }

    if (applicant.getId().equals(animalCase.getCurrentFoster().getId())) {
      throw new CustomException(ErrorCode.ALREADY_FOSTER);
    }

    // 기존에 수락 대기 신청이 있는지 확인
    boolean hasPendingApplication = protectionRepository
        .existsByApplicantIdAndAnimalCaseIdAndProtectionStatusAndDeletedAtIsNull(
            memberId, caseId, ProtectionStatus.PENDING);
    if (hasPendingApplication) {
      throw new CustomException(ErrorCode.ALREADY_APPLIED);
    }

    Protection protection = Protection.builder()
        .applicant(applicant)
        .animalCase(animalCase)
        .reason(reason)
        .protectionType(protectionType)
        .protectionStatus(ProtectionStatus.PENDING)
        .build();

    protectionRepository.save(protection);
    animalCaseEventPublisher.applyProtection(protection, memberId, animalCase.getStatus());
    return ProtectionResponse.of(protection);
  }



  @Transactional
  public void cancelProtection(Long protectionId, Long memberId) {
    Protection protection = protectionRepository.findById(protectionId)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    // 내가 작성한 것인지 확인
    if (!protection.getApplicant().getId().equals(memberId)) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    // 이미 취소할 수 없는 상태인지 확인
    if (protection.getProtectionStatus() != ProtectionStatus.PENDING) {
      throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }

    protection.setProtectionStatus(ProtectionStatus.CANCELED);
    protection.cancel();
  }



  @Transactional
  public void acceptProtection(Long protectionId, Long memberId) {
    Protection protection = protectionRepository.findByIdWithFetchAll(protectionId)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    if (protection.getProtectionStatus() != ProtectionStatus.PENDING) {
      throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }

    if (!protection.getAnimalCase().getCurrentFoster().getId().equals(memberId)) { // 권한 검사
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    protection.approve();
    AnimalCase animalCase = protection.getAnimalCase();
    animalCase.updateStatus(CaseStatus.TEMP_PROTECTING);
    if (protection.getProtectionType().equals(ProtectionType.ADOPTION)) {
      animalCase.updateStatus(CaseStatus.ADOPTED);
    }

    animalCase.getAnimal().setOwner(protection.getApplicant());
    animalCase.setCurrentFoster(protection.getApplicant());
    animalCaseEventPublisher.acceptProtection(protection, memberId, animalCase.getStatus());
  }


  @Transactional
  public void rejectProtection(Long protectionId, Long memberId, String rejectReason) {
    Protection protection = protectionRepository.findByIdWithFetchAll(protectionId)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    if (protection.getProtectionStatus() != ProtectionStatus.PENDING) {  // 상태 검증
      throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }

    if (protection.getAnimalCase().getStatus() != CaseStatus.PROTECT_WAITING) {  // 케이스 상태 검증
      throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }

    if (!protection.getAnimalCase().getCurrentFoster().getId().equals(memberId)) { // 권한 검사
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    protection.reject(rejectReason);
    animalCaseEventPublisher.rejectProtection(protection.getId(), memberId, protection.getAnimalCase().getStatus());
  }


  @Transactional
  public void createAnimalCase(
      CreateAnimalCaseRequest request, Member member, List<MultipartFile> images
  ) {
    Animal animal = request.toAnimal();
    animalRepository.save(animal);

    if (images != null && !images.isEmpty()) {
      List<Image> imageList = imageService.uploadAnimalImages(images, animal.getId());
      animal.setImageUrl(imageList.getFirst().getPath());
    }


    animalCaseEventPublisher.createAnimalCase(member, animal, request.title(), request.description());
  }


  private List<PendingProtectionResponse> getPendingProtections(Long animalCaseId) {
    return protectionRepository
        .findAllByAnimalCaseIdAndProtectionStatusAndDeletedAtIsNull(animalCaseId, ProtectionStatus.PENDING)
        .stream()
        .map(PendingProtectionResponse::of)
        .toList();
  }

  @Transactional
  public void updateAnimalCase(Long caseId, UpdateAnimalCaseRequest request, Member member, List<MultipartFile> images) {
    Collection<CaseStatus> possibleStatuses = List.of(
        CaseStatus.PROTECT_WAITING,
        CaseStatus.TEMP_PROTECTING,
        CaseStatus.SHELTER_PROTECTING
    );
    AnimalCase animalCase = animalCaseService.findByIdAndStatusesWithHistories(caseId, possibleStatuses)
        .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

    if (!animalCase.getCurrentFoster().getId().equals(member.getId())) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }

    Animal animal = request.updateAnimal(animalCase);

    if (images != null && !images.isEmpty()) {
      List<Image> imageList = imageService.uploadAnimalImages(images, animal.getId());
      animal.setImageUrl(imageList.getFirst().getPath());
    }
  }

  @Transactional
  public void deleteAnimalCase(Long caseId, Long memberId) {
    AnimalCase animalCase = animalCaseService.findById(caseId);
    if (!animalCase.getCurrentFoster().getId().equals(memberId)) {
      throw new CustomException(ErrorCode.UNAUTHORIZED_ACCESS);
    }
    animalCaseService.softDeleteAnimalCase(animalCase, memberId);
  }
}
