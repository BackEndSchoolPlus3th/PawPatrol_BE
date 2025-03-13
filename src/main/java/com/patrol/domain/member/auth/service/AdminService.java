package com.patrol.domain.member.auth.service;

import com.patrol.api.member.member.dto.GetAllMembersResponse;
import com.patrol.api.member.member.dto.request.ChangeMemberStatusRequest;
import com.patrol.domain.facility.repository.ShelterRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.repository.V2MemberRepository;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * packageName    : com.patrol.domain.member.auth.service
 * fileName       : AdminService
 * author         : sungjun
 * date           : 2025-03-05
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-03-05        kyd54       최초 생성
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {
    private final V2MemberRepository v2MemberRepository;
    private final Logger logger = LoggerFactory.getLogger(AdminService.class.getName());
    private final ShelterRepository shelterRepository;

    // 모든 회원 정보 가져오기
    @Transactional
    public Page<GetAllMembersResponse> getAllMembers(Pageable pageable) {
        logger.info("모든 회원 정보 가져오기 : getAllMembers");

        // ROLE_ADMIN이 아닌 회원만 조회
        Page<Member> members = v2MemberRepository.findByRoleNot(MemberRole.ROLE_ADMIN, pageable);

        return members.map(member -> GetAllMembersResponse.builder()
                .id(member.getId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .createdAt(member.getCreatedAt())
                .status(member.getStatus())
                .role(member.getRole())
                .build());
    }

    // 관리자 > 회원 상태 변경
    public void changeMemberStatus(ChangeMemberStatusRequest changeMemberStatusRequest) {
        logger.info("회원 상태 변경 : changeMemberStatus");
        Member member = v2MemberRepository.findById(changeMemberStatusRequest.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        // 요청된 상태로 직접 변경
        member.setStatus(changeMemberStatusRequest.status());
    }
}
