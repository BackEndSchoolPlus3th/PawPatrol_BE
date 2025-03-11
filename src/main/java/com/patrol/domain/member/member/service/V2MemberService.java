package com.patrol.domain.member.member.service;

import com.patrol.api.member.auth.dto.ModifyProfileResponse;
import com.patrol.api.member.auth.dto.MyPostsResponse;
import com.patrol.api.member.auth.dto.requestV2.ModifyProfileRequest;
import com.patrol.api.member.member.dto.GetAllMembersResponse;
import com.patrol.api.member.member.dto.OAuthConnectInfoResponse;
import com.patrol.domain.lostFoundPost.service.LostFoundPostService;
import com.patrol.domain.member.auth.entity.OAuthProvider;
import com.patrol.domain.member.auth.repository.OAuthProviderRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.repository.V2MemberRepository;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import com.patrol.global.storage.FileStorageHandler;
import com.patrol.global.storage.FileUploadRequest;
import com.patrol.global.storage.FileUploadResult;
import com.patrol.global.storage.StorageConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * packageName    : com.patrol.domain.member.member.service
 * fileName       : V2MemberService
 * author         : sungjun
 * date           : 2025-02-19
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-19        kyd54       최초 생성
 */
@Service
@RequiredArgsConstructor
@Transactional
public class V2MemberService {
    private final V2MemberRepository v2MemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageHandler fileStorageHandler;
    private final LostFoundPostService lostFoundPostService;
    private final OAuthProviderRepository oAuthProviderRepository;

    private final Logger logger = LoggerFactory.getLogger(V2MemberService.class.getName());

    // 회원 정보 가져오기
    @Transactional
    public Member getMember(String email) {
        logger.info("회원 정보 가져오기_getMember");
        return v2MemberRepository.findByEmail(email)
                // 이거 어떻게 바꿔야 하는지
                .orElseThrow(() -> new ServiceException(ErrorCodes.INVALID_EMAIL));
    }

    // 마이페이지 > 프로필 이미지 삭제
    @Transactional
    public void resetProfileImage(Member member, ModifyProfileRequest modifyProfileRequest) {
        logger.info("마이페이지 > 프로필 이미지 삭제 : resetProfileImage");
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

//        String objectKey = modifyProfileRequest.imageUrl().replace("https://kr.object.ncloudstorage.com/paw-patrol/", "");
        // 기존 파일 삭제
        fileStorageHandler.handleFileDelete(modifyMem.getProfileImageUrl());
        
        // 프로필을 디폴트 이미지로 변경
        if(modifyProfileRequest.file() == null && !modifyProfileRequest.imageUrl().isEmpty()) {
            modifyMem.setProfileImageUrl("default.png");
        }
    }

    // 마이페이지 >> 프로필 이미지 수정
    @Transactional
    public ModifyProfileResponse modifyProfileImage(
            Member member,
            ModifyProfileRequest modifyProfileRequest
    ) {
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

        // 프로필 이미지 변경
        if (modifyProfileRequest.file() != null && !modifyProfileRequest.file().isEmpty()) {
            logger.info("회원 정보 수정 - 프로필 이미지 변경");

            // 기본 이미지가 아닐때
            String fileName = modifyProfileRequest.imageUrl();
            int lastSlashIndex = fileName.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                fileName = fileName.substring(lastSlashIndex + 1);
            }

            if (!fileName.equals("default.png")) {
                // 기존 파일 삭제
                fileStorageHandler.handleFileDelete(modifyProfileRequest.imageUrl());
            }

            // 이미지 업로드
            FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                    FileUploadRequest.builder()
                            .folderPath("profile/" + member.getId())
                            .file(modifyProfileRequest.file())
                            .build()
            );

            // 이미지 URL 업데이트
            modifyMem.setProfileImageUrl(uploadResult.getFullPath());
        }

        return ModifyProfileResponse.builder()
                .profileImage(modifyMem.getProfileImageUrl())
                .build();
    }

    // 회원 정보 수정 > 전화번호 수정 시 인증 필요함
    @Transactional
    public void modifyProfile(Member member,
                                               ModifyProfileRequest modifyProfileRequest) {
        logger.info("회원 정보 수정_modifyProfile");
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

        // 닉네임 변경
        if(modifyProfileRequest.nickname() != null) {
            logger.info("회원 정보 수정 - 닉네임 변경");
            modifyMem.updateNickname(modifyProfileRequest.nickname());
        }
        // 비밀번호 변경
        if (modifyProfileRequest.currentPassword() != null
                && modifyProfileRequest.newPassword() != null
                && modifyProfileRequest.confirmPassword() != null) {
            logger.info("회원 정보 수정 - 비밀번호 변경");
            // 비밀번호 검증 로직
            // 현재 비밀번호와 일치하는지
            if (!passwordEncoder.matches(modifyProfileRequest.currentPassword(), member.getPassword())) {
                throw new ServiceException(ErrorCodes.CURRENT_PASSWORD_NOT_MATCH);
            }
            // 새 비밀번호와 비밀번호 확인이 일치하는지
            if (!modifyProfileRequest.confirmPassword().equals(modifyProfileRequest.newPassword())) {
                throw new ServiceException(ErrorCodes.INVALID_PASSWORD);
            }

            modifyMem.updatePassword(passwordEncoder.encode(modifyProfileRequest.newPassword()));
        }

        // 전화번호 인증, 변경
        if (modifyProfileRequest.phoneNumber() != null) {
            logger.info("회원 정보 수정 - 전화번호 변경");
            modifyMem.updatePhoneNum(modifyProfileRequest.phoneNumber());
        }
    }

    // 소셜 로그인 연동 시, 자체 계정 유무 확인
    @Transactional
    public boolean validateNewEmail(String email) {
        logger.info("소셜 로그인 연동 시 자체 계정 유무 확인_validateNewEmail");
        return v2MemberRepository.findByEmail(email).isPresent();
    }

    // 마이페이지 나의 신고글 리스트 불러오기
    @Transactional
    public Page<MyPostsResponse> myReportPosts(Member member, Pageable pageable) {
        return lostFoundPostService.myReportPosts(member, pageable);
    }
    // 마이페이지 나의 제보글 리스트 불러오기
    @Transactional
    public Page<MyPostsResponse> myWitnessPosts(Member member, Pageable pageable) {
        return lostFoundPostService.myWitnessPosts(member, pageable);
    }

    // 소셜 로그인 연결 확인
    @Transactional
    public OAuthConnectInfoResponse socialInfo(Member member) {
        OAuthProvider authProvider = oAuthProviderRepository.findByMemberId(member.getId());

        boolean isNaverConnected = false;
        boolean isGoogleConnected = false;
        boolean isKakaoConnected = false;

        try {
            if (authProvider.getNaver() != null) {
                isNaverConnected = authProvider.getNaver().isConnected();
            }
            if (authProvider.getGoogle() != null) {
                isGoogleConnected = authProvider.getGoogle().isConnected();
            }
            if (authProvider.getKakao() != null) {
                isKakaoConnected = authProvider.getKakao().isConnected();
            }

            return OAuthConnectInfoResponse.builder()
                    .naver(isNaverConnected)
                    .google(isGoogleConnected)
                    .kakao(isKakaoConnected)
                    .build();
        } catch (NullPointerException e) {
            logger.info("OAuth2 연동 X");
            return OAuthConnectInfoResponse.builder()
                    .naver(isNaverConnected)
                    .google(isGoogleConnected)
                    .kakao(isKakaoConnected)
                    .build();
        }
    }

    // 회원 탈퇴
    @Transactional
    public void memberWithdraw(Member member) {
        logger.info("회원 탈퇴 : memberWithdraw");
        Member inActiveMember = v2MemberRepository.findById(member.getId()).orElseThrow();

        inActiveMember.setStatus(MemberStatus.WITHDRAWN);
    }

}
