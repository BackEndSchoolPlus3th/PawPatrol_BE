package com.patrol.domain.member.auth.service;

import com.patrol.api.member.auth.dto.SocialTokenInfo;
import com.patrol.api.member.auth.dto.request.SignupRequest;
import com.patrol.api.member.auth.dto.requestV2.*;
import com.patrol.domain.facility.entity.Shelter;
import com.patrol.domain.facility.repository.ShelterRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.domain.member.member.repository.V2MemberRepository;
import com.patrol.domain.member.member.service.V2MemberService;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import com.patrol.global.rq.Rq;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class V2AuthService {
    @Value("${custom.shelter.baseUrl}")
    private String baseUrl;
    @Value("${custom.shelter.serviceKey}")
    private String serviceKey;

    private final Logger logger = LoggerFactory.getLogger(V2AuthService.class.getName());
    private final V2MemberRepository v2MemberRepository;
    private final V2MemberService v2MemberService;
    private final OAuthService oAuthService;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService authTokenService;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final Rq rq;
    private final ShelterRepository shelterRepository;

    private static final String KEY_PREFIX = "find:verification:";

    @Transactional
    public Member signUp(SignupRequest request) {
        logger.info("회원가입 : signUp");

        if (v2MemberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .address(request.address())
                .apiKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .build();

        v2MemberRepository.save(member);

        return member;
    }


    @Transactional
    public String login(LoginRequest loginRequest) {
        Member member = v2MemberService.getMember(loginRequest.email());
        if (member.getStatus() == MemberStatus.ACTIVE) {
            if (!passwordEncoder.matches(loginRequest.password(), member.getPassword())) {
                throw new ServiceException(ErrorCodes.INVALID_PASSWORD);
            }
            return rq.makeAuthCookies(member);
        }

        throw new CustomException(ErrorCode.RESTRICTED_ACCOUNT_ACCESS);
    }

    @Transactional
    public Member handleSocialLogin(
                                        String email,
                                        ProviderType loginType, String providerId
    ) {
        logger.info("소셜 로그인 시, 사이트 자체 계정의 유무에 따른 처리_handleSocialLogin");
        Member connectedMember = oAuthService.findByProviderId(loginType, providerId);
        if (connectedMember != null) {
            connectedMember.setLoginType(loginType);
            return connectedMember;
        }
        String tempToken = authTokenService.generateTempSocialToken(email, loginType, providerId);

        throw new OAuth2AuthenticationException(
                new OAuth2Error("temp_token", tempToken, null)
        );
    }

    // 소셜 계정 연동
    @Transactional
    public void socialConnect(@Valid SocialConnectRequest socialConnectRequest,
                              String accessToken) {
        logger.info("소셜 계정 연동_socialConnect");
        Map<String, Object> loginUser = authTokenService.payload(accessToken);
        SocialTokenInfo socialTokenInfo = authTokenService.parseSocialToken(socialConnectRequest.tempToken());

        Long userId = (Long)loginUser.get("id");

        Member member = v2MemberRepository.findById(userId).orElseThrow();

        connectOAuthProvider(
                member,
                socialTokenInfo.getProviderType(),
                socialTokenInfo.getProviderId(),
                socialTokenInfo.getEmail()
        );
    }

    // 엑세스 토큰 발행
    public String genAccessToken(Member member) {
        logger.info("엑세스 토큰 발행_genAccessToken");
        return authTokenService.genAccessToken(member);
    }

    /**
     * 기존 계정(loginUser)에 소셜 계정 정보를 연동
     * @param loginUser 연동할 기존 계정
     * @param loginType 소셜 로그인 제공자 타입 (KAKAO, GOOGLE, NAVER)
     * @param providerId 소셜 계정의 고유 ID
     * @param providerEmail 소셜 계정의 이메일
     * @throws ServiceException 이미 다른 계정과 연동된 소셜 계정인 경우
     */
    @Transactional
    public void connectOAuthProvider(
            Member loginUser, ProviderType loginType, String providerId, String providerEmail
    ) {
        logger.info("기존 계정에 소셜 계정 정보 연동 : connectOAuthProvider");
        Member connectedMember = oAuthService.findByProviderId(loginType, providerId);
        if (connectedMember != null) {
            throw new ServiceException(ErrorCodes.SOCIAL_ACCOUNT_ALREADY_IN_USE);
        }
        oAuthService.connectProvider(loginUser, loginType, providerId, providerEmail);
    }

    @Transactional
    public Optional<Member> findByApiKey(String apiKey) {
        return v2MemberRepository.findByApiKey(apiKey);
    }

    // 비밀번호 찾기, 토큰 발행 (aka. 비찾토발)
    // 비밀번호 재설정 과정에서 보안 토큰을 발행하여 권한이 없는 사용자가 우회하여 접근하지 못하게 막는 로직
    @Transactional
    public Map<String, String> resetToken(String email) {
        logger.info("비밀번호 찾기, 토큰 발행 (aka. 비찾토발) : resetToken");
        String continuationToken = UUID.randomUUID().toString();

        _saveContinuationToken(email, continuationToken);

        Map<String, String> response = new HashMap<>();
        response.put("continuationToken", continuationToken);

        return response;
    }

    @Transactional
    public void _saveContinuationToken(String email, String token) {
        logger.info("인증 토큰 저장 (Redis : 10분 유효, (aka. 비찾토발)) : _saveContinuationToken");
        String key = KEY_PREFIX + email;

        redisTemplate.opsForValue()
                .set(key, token, Duration.ofMinutes(10));  // TTL 설정 추가
    }

    @Transactional
    public boolean _validateContinuationToken(String email, String continuationToken) {
        String key = KEY_PREFIX + email;
        String savedToken = redisTemplate.opsForValue().get(key);

        if (savedToken == null) {
            throw new CustomException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        if (!savedToken.equals(continuationToken)) {
            throw new CustomException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        redisTemplate.delete(key);
        redisTemplate.opsForValue()
                .set("email:verify:" + email, "verified", 3, TimeUnit.MINUTES);

        return true;
    }

    @Transactional
    public void deleteToken(String email) {
        String key = KEY_PREFIX + email;
        redisTemplate.delete(key);
    }

    @Transactional
    public void resetPassword(NewPasswordRequest request) {
        if (request.newPassword() != null
                && request.confirmPassword() != null) {
            logger.info("비밀번호찾기 - 비밀번호 재설정");

            Member member = v2MemberRepository.findByEmail(request.email()).orElseThrow();

            if (!request.confirmPassword().equals(request.newPassword())) {
                throw new ServiceException(ErrorCodes.INVALID_PASSWORD);
            }

            member.updatePassword(passwordEncoder.encode(request.newPassword()));
        }
    }

    public String validateBusinessNumber(BusinessNumberRequest request) throws Exception {
        String fullUrl = baseUrl + "?serviceKey=" + serviceKey;
        URI uri = new URI(fullUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("businesses", List.of(
                Map.of(
                        "b_no", request.businessNumber(),
                        "start_dt", request.startDate(),
                        "p_nm", request.owner()
                )
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new RuntimeException("API call failed with status: " + response.getStatusCodeValue());
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            logger.error("API call failed. Status code: {}, Response body: {}", e.getRawStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("API call failed", e);
        }
    }

    public Member shelterSignUp(ShelterSignupRequest request) {
        logger.info("보호소 회원가입 : shelterSignUp");

        if (v2MemberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (shelterRepository.existsByBusinessRegistrationNumber(request.businessRegistrationNumber())) {
            throw new CustomException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.builder()
                .email(request.email())
                .password(encodedPassword)
                .nickname(request.nickname()) // 여기서 닉네임은 사업장명임
                .address(request.address())
                .status(MemberStatus.ACTIVE)
                .role(MemberRole.ROLE_SHELTER) // 보호소 역할 부여
                .loginType(ProviderType.SELF)
                .apiKey(UUID.randomUUID().toString())
                .marketingAgree(false) // 기본값
                .build();

        Member savedMember = v2MemberRepository.save(member);

        try {
            if (request.shelterId() == null) {
                Shelter shelter = Shelter.builder()
                        .shelterMember(savedMember)
                        .name(request.nickname()) // 기본 사업장명 설정
                        .owner(request.owner())
                        .address(request.address())
                        .businessRegistrationNumber(request.businessRegistrationNumber())
                        .build();

                shelterRepository.save(shelter);

                savedMember.setShelter(shelter);
            } else {
                Optional<Shelter> shelterOptional = shelterRepository.findById(request.shelterId());

                if (shelterOptional.isPresent()) {
                    Shelter isShelter = shelterOptional.get();

                    if (isShelter.getShelterMember() != null) {
                        throw new CustomException(ErrorCode.DUPLICATE_SHELTER_MEMBER);
                    }

                    savedMember.setShelter(isShelter);
                    isShelter.setShelterMember(savedMember);
                } else {
                    logger.error("존재하지 않는 보호소 ID: {}", request.shelterId());
                    throw new CustomException(ErrorCode.SHELTER_NOT_FOUND);
                }
            }
        } catch (CustomException e) {
            v2MemberRepository.delete(savedMember);
            throw e;
        }

        logger.info("보호소 회원가입 완료: {}", savedMember.getEmail());
        return savedMember;
    }

}
