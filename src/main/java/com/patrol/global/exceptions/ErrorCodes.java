package com.patrol.global.exceptions;

import lombok.Getter;

@Getter
public enum ErrorCodes {

  // Auth 관련 에러 (400번대)
  INVALID_AUTH_TOKEN(401, "AUTH_001", "유효하지 않은 토큰입니다."),
  TOKEN_EXPIRED(401, "AUTH_002", "만료된 토큰입니다."),
  SOCIAL_ONLY_ACCOUNT(401, "AUTH_003", "소셜 로그인으로만 가입된 계정입니다. 비밀번호를 먼저 설정해주세요."),
  INVALID_PASSWORD(401, "AUTH_004", "비밀번호가 일치하지 않습니다."),
  INVALID_EMAIL(404, "AUTH_005", "해당 이메일로 가입된 정보가 없습니다."),
  UNAUTHORIZED(401, "AUTH_006", "인증된 사용자 정보가 없습니다."),
  REDIRECT_URL_NOT_FOUND(400, "AUTH_007", "리다이렉트 URL을 찾을 수 없습니다."),
  PASSWORD_REQUIRED(400, "AUTH_008", "비밀번호는 필수 입력값입니다."),
  EMAIL_REQUIRED(400, "AUTH_009", "이메일은 필수 입력값입니다."),
  CURRENT_PASSWORD_NOT_MATCH(400, "AUTH_010", "현재 비밀번호가 일치하지 않습니다."),
  PASSWORD_CHANGE_FAILED(500, "AUTH_011", "비밀번호 변경에 실패했습니다."),
  EMAIL_NOT_VERIFIED(400, "AUTH_012", "이메일 인증을 완료 후 회원가입 진행해주세요."),
  SOCIAL_ACCOUNT_ALREADY_IN_USE(400, "AUTH_013", "이미 사용중인 소셜 계정입니다."),

  // Member 관련 에러
  MEMBER_NOT_FOUND(404, "MEMBER_001", "사용자를 찾을 수 없습니다."),
  DUPLICATE_EMAIL(409, "MEMBER_002", "이미 사용중인 이메일입니다."),
  INVALID_LOGIN_TYPE(400, "MEMBER_003", "지원하지 않는 로그인 타입입니다."),
  INVALID_ROLE(400, "MEMBER_004", "권한이 없습니다."),
  PASSWORD_RESET_NOT_VERIFIED(400, "MEMBER_005", "비밀번호 재설정 인증이 필요합니다."),
  INVALID_PHONE_NUMBER(400, "MEMBER_006", "해당 전화번호로 등록된 계정을 찾을 수 없습니다."),
  ALREADY_CONNECTED_SOCIAL_ACCOUNT(400, "MEMBER_007", "이미 연결된 소셜 계정입니다."),
  NOT_CONNECTED_SOCIAL_ACCOUNT(400, "MEMBER_008", "연결되지 않은 소셜 계정입니다."),
  ALREADY_HAS_PASSWORD(400, "MEMBER_009", "이미 비밀번호가 설정되어 있습니다."),
  CANNOT_DISCONNECT_LAST_LOGIN_METHOD(400, "MEMBER_010", "마지막 로그인 수단은 연결을 해제할 수 없습니다."),
  DISCONNECT_FAIL(500, "MEMBER_011", "소셜 계정 연동 해제에 실패했습니다."),
  INVALID_SOCIAL_CONNECTION(400, "MEMBER_012", "기존 정보를 찾지 못하여 소셜 연동이 실패했습니다."),
  PASSWORD_VERIFICATION_REQUIRED(400, "MEMBER_013", "비밀번호 확인이 필요합니다."),

  // Email 관련 에러
  EMAIL_SEND_FAIL(500, "EMAIL_001", "이메일 전송에 실패했습니다."),
  EMAIL_VERIFICATION_NOT_FOUND(404, "EMAIL_002", "인증 코드가 존재하지 않습니다."),
  EMAIL_VERIFICATION_EXPIRED(400, "EMAIL_003", "만료된 인증 코드입니다."),
  EMAIL_VERIFICATION_NOT_MATCH(400, "EMAIL_004", "인증 코드가 일치하지 않습니다."),

  // SMS 관련 에러
  SMS_SEND_FAILED(500, "SMS_001", "SMS 발송에 실패했습니다."),
  SMS_CODE_EXPIRED(400, "SMS_002", "만료된 인증번호입니다."),
  SMS_RESEND_TOO_EARLY(400, "SMS_003", "재전송 대기 시간이 아직 남았습니다."),
  SMS_DAILY_LIMIT_EXCEEDED(400, "SMS_004", "하루 최대 전송 횟수를 초과했습니다."),
  INVALID_SNS_VERIFICATION_CODE(400, "SMS_005", "인증번호가 일치하지 않습니다."),
  PHONE_NUMBER_NOT_VERIFIED(400, "SMS_006", "휴대폰 번호는 인증을 완료 후 추가할 수 있습니다."),
  ALREADY_VERIFIED_PHONE_NUMBER(400, "SMS_007", "이미 인증된 휴대폰 번호입니다."),

  // LostPost 관련 에러
  LOST_POST_NOT_FOUND(404, "LOST_001", "실종 게시글을 찾을 수 없습니다."),
  LOST_POST_CREATION_FAILED(500, "LOST_002", "실종 게시글 생성에 실패했습니다."),
  LOST_POST_UPDATE_FAILED(500, "LOST_003", "실종 게시글 수정에 실패했습니다."),
  LOST_POST_DELETION_FAILED(500, "LOST_004", "실종 게시글 삭제에 실패했습니다."),

  // FindPost 관련 에러
  FIND_POST_NOT_FOUND(404, "FIND_001", "제보 게시글을 찾을 수 없습니다."),
  FIND_POST_CREATION_FAILED(500, "FIND_002", "제보 게시글 생성에 실패했습니다."),
  FIND_POST_UPDATE_FAILED(500, "FIND_003", "제보 게시글 수정에 실패했습니다."),
  FIND_POST_DELETION_FAILED(500, "FIND_004", "제보 게시글 삭제에 실패했습니다."),
  FIND_POST_CONNECTION_FAILED(400, "FIND_005", "실종 게시글과 제보 게시글의 연결에 실패했습니다."),

  // Comment 관련 에러
  COMMENT_NOT_FOUND(404, "COMMENT_001", "댓글을 찾을 수 없습니다."),
  COMMENT_CREATION_FAILED(500, "COMMENT_002", "댓글 생성에 실패했습니다."),
  COMMENT_UPDATE_FAILED(500, "COMMENT_003", "댓글 수정에 실패했습니다."),
  COMMENT_DELETION_FAILED(500, "COMMENT_004", "댓글 삭제에 실패했습니다."),
  COMMENT_NOT_ASSOCIATED_WITH_POST(400, "COMMENT_005", "댓글은 실종 신고 또는 제보 게시글에만 작성할 수 있습니다."),

  // Business 에러 (500번대)
  INTERNAL_ERROR(500, "SYS_001", "내부 시스템 에러"),
  API_CALL_FAILED(500, "SYS_002", "외부 API 호출에 실패했습니다."),
  TYPE_TRANSFER_FAILED(400, "SYS_003", "지원하지 않는 로그인 타입");


  private final int status;
  private final String code;
  private final String message;

  ErrorCodes (int status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
