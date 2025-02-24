package com.patrol.domain.animalCase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CaseHistoryStatus {
  // 게시글
  INITIAL_MISSING_REPORT("최초 실종 신고"), // 처음 실종 신고했을 때
  ADDITIONAL_MISSING_REPORT("추가 실종 신고"), // 이미 신고된 실종 건에 추가 신고할 때
  FOUND_REPORT("발견 제보"), // 누군가 발견했다고 제보
  RESCUE_REPORT("구조 제보"), // 누군가 구조했다고 제보

  // 임시보호 관련
  TEMP_PROTECT_REGISTERED("임시보호 등록"),
  TEMP_PROTECT_WAIT("임시보호 대기"), // 임시보호 대기
  TEMP_PROTECT_REQUEST("임시보호 신청"), // 임시보호 신청
  TEMP_PROTECT_APPROVED("임시보호 승인(시작)"), // 임시보호 승인
  TEMP_PROTECT_REJECTED("임시보호 거절"), // 임시보호 거절
  TEMP_PROTECT_END("임시보호 종료"), // 임시보호 종료

  // 종료 관련
  OWNER_FOUND("보호자 인계"), // 원래 주인 찾음
  ADOPTED("입양 완료"); // 새로운 가정에 입양

  private final String description;

}
