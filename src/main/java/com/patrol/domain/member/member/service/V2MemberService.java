package com.patrol.domain.member.member.service;

import com.patrol.api.member.auth.dto.ModifyProfileResponse;
import com.patrol.api.member.auth.dto.MyPostsResponse;
import com.patrol.api.member.auth.dto.requestV2.ModifyProfileRequest;
import com.patrol.api.member.member.dto.OAuthConnectInfoResponse;
import com.patrol.domain.lostFoundPost.service.LostFoundPostService;
import com.patrol.domain.member.auth.entity.OAuthProvider;
import com.patrol.domain.member.auth.repository.OAuthProviderRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.repository.V2MemberRepository;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import com.patrol.global.storage.FileStorageHandler;
import com.patrol.global.storage.FileUploadRequest;
import com.patrol.global.storage.FileUploadResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    @Transactional
    public Member getMember(String email) {
        return v2MemberRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceException(ErrorCodes.INVALID_EMAIL));
    }

    @Transactional
    public void resetProfileImage(Member member, ModifyProfileRequest modifyProfileRequest) {
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

        fileStorageHandler.handleFileDelete(modifyMem.getProfileImageUrl());

        if(modifyProfileRequest.file() == null && !modifyProfileRequest.imageUrl().isEmpty()) {
            modifyMem.setProfileImageUrl("default.png");
        }
    }

    @Transactional
    public ModifyProfileResponse modifyProfileImage(
            Member member,
            ModifyProfileRequest modifyProfileRequest
    ) {
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

        if (modifyProfileRequest.file() != null && !modifyProfileRequest.file().isEmpty()) {

            String fileName = modifyProfileRequest.imageUrl();
            int lastSlashIndex = fileName.lastIndexOf('/');
            if (lastSlashIndex != -1) {
                fileName = fileName.substring(lastSlashIndex + 1);
            }

            if (!fileName.equals("default.png")) {
                fileStorageHandler.handleFileDelete(modifyProfileRequest.imageUrl());
            }

            FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                    FileUploadRequest.builder()
                            .folderPath("profile/" + member.getId())
                            .file(modifyProfileRequest.file())
                            .build()
            );

            modifyMem.setProfileImageUrl(uploadResult.getFullPath());
        }

        return ModifyProfileResponse.builder()
                .profileImage(modifyMem.getProfileImageUrl())
                .build();
    }

    @Transactional
    public void modifyProfile(Member member,
                                               ModifyProfileRequest modifyProfileRequest) {
        Member modifyMem = v2MemberRepository.findByEmail(member.getEmail()).orElseThrow();

        if(modifyProfileRequest.nickname() != null) {
            modifyMem.updateNickname(modifyProfileRequest.nickname());
        }
        if (modifyProfileRequest.currentPassword() != null
                && modifyProfileRequest.newPassword() != null
                && modifyProfileRequest.confirmPassword() != null) {
            if (!passwordEncoder.matches(modifyProfileRequest.currentPassword(), member.getPassword())) {
                throw new ServiceException(ErrorCodes.CURRENT_PASSWORD_NOT_MATCH);
            }
            if (!modifyProfileRequest.confirmPassword().equals(modifyProfileRequest.newPassword())) {
                throw new ServiceException(ErrorCodes.INVALID_PASSWORD);
            }

            modifyMem.updatePassword(passwordEncoder.encode(modifyProfileRequest.newPassword()));
        }

        if (modifyProfileRequest.phoneNumber() != null) {
            modifyMem.updatePhoneNum(modifyProfileRequest.phoneNumber());
        }
    }

    @Transactional
    public boolean validateNewEmail(String email) {
        return v2MemberRepository.findByEmail(email).isPresent();
    }

    @Transactional
    public Page<MyPostsResponse> myReportPosts(Member member, Pageable pageable) {
        return lostFoundPostService.myReportPosts(member, pageable);
    }

    @Transactional
    public Page<MyPostsResponse> myWitnessPosts(Member member, Pageable pageable) {
        return lostFoundPostService.myWitnessPosts(member, pageable);
    }

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

    @Transactional
    public void memberWithdraw(Member member) {
        Member inActiveMember = v2MemberRepository.findById(member.getId()).orElseThrow();

        inActiveMember.setStatus(MemberStatus.WITHDRAWN);
    }

}
