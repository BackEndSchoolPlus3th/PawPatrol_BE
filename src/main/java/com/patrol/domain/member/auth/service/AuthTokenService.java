package com.patrol.domain.member.auth.service;

import com.patrol.api.member.auth.dto.SocialTokenInfo;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.global.exceptions.ErrorCodes;
import com.patrol.global.exceptions.ServiceException;
import com.patrol.standard.util.Ut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthTokenService {

  @Value("${custom.jwt.secretKey}")
  private String jwtSecretKey;

  @Value("${custom.accessToken.expirationSeconds}")
  private long accessTokenExpirationSeconds;

  @Value("${custom.socialToken.expirationSeconds}")
  private long socialTokenExpirationSeconds;


  String genAccessToken(Member member) {
    long id = member.getId();
    String email = member.getEmail();
    String nickname = member.getNickname();
    String profileImageUrl = member.getProfileImageUrl();
    String role = member.getRole().name();

    Map<String, Object> payload = new HashMap<>();
    payload.put("id", id);
    payload.put("email", email != null ? email : "");
    payload.put("nickname", nickname != null ? nickname : "");
    payload.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
    payload.put("role", role);

    return Ut.jwt.toString(
            jwtSecretKey,
            accessTokenExpirationSeconds,
            payload
    );
  }

  // 엑세스 토큰 파싱
  public Map<String, Object> payload(String accessToken) {
    Map<String, Object> parsedPayload = Ut.jwt.payload(jwtSecretKey, accessToken);
    if (parsedPayload == null) {
      return null;
    }
    long id = (long) (Integer) parsedPayload.get("id");
    String email = (String) parsedPayload.get("email");
    String nickname = (String) parsedPayload.get("nickname");
    String profileImageUrl = (String) parsedPayload.get("profileImageUrl");
    String role = (String) parsedPayload.get("role");

    return Map.of(
        "id", id, "email", email, "nickname", nickname,
        "profileImageUrl", profileImageUrl, "role", role
    );
  }

  // 소셜 로그인 정보 토큰 발행
  public String generateTempSocialToken(String email, ProviderType providerType, String providerId) {
    try {
      return Ut.jwt.toSocialString(
              jwtSecretKey,
              socialTokenExpirationSeconds,
              providerId,
              providerType.toString(),
              email
      );
    } catch (Exception e) {
      throw new ServiceException(ErrorCodes.TYPE_TRANSFER_FAILED);
    }
  }

  // 소셜 로그인 정보 토큰 파싱
  public SocialTokenInfo parseSocialToken(String token) {
    try {
      Map<String, Object> parsedPayload = Ut.jwt.payload(jwtSecretKey, token);

      if (parsedPayload == null || !"SOCIAL_TEMP".equals(parsedPayload.get("type"))) {
        return null;
      }

      return SocialTokenInfo.builder()
              .providerType(ProviderType.of((String) parsedPayload.get("loginType")))
              .providerId((String) parsedPayload.get("providerId"))
              .email((String) parsedPayload.get("email"))
              .build();
    } catch (Exception e) {
      throw new ServiceException(ErrorCodes.TYPE_TRANSFER_FAILED);
    }
  }

}
