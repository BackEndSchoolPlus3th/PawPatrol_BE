package com.patrol.domain.kakao.service;

import com.patrol.api.kakao.dto.KakaoAddressResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class KakaoApiService {

  private final RestTemplate restTemplate;

  @Value("${kakao.rest-api-key}")
  private String kakaoRestApiKey;

  private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

  public KakaoAddressResponse getAddressFromCoords(double longitude, double latitude) {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

    URI uri = UriComponentsBuilder.fromHttpUrl(KAKAO_API_URL)
        .queryParam("x", longitude)
        .queryParam("y", latitude)
        .build()
        .toUri();

    HttpEntity<?> entity = new HttpEntity<>(headers);

    ResponseEntity<KakaoAddressResponse> response = restTemplate.exchange(
        uri,
        HttpMethod.GET,
        entity,
        KakaoAddressResponse.class
    );

    return response.getBody();
  }
}
