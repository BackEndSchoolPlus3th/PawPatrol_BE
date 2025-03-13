package com.patrol.api.kakao.controller;

import com.patrol.api.kakao.dto.KakaoAddressResponse;
import com.patrol.domain.kakao.service.KakaoApiService;
import com.patrol.global.rsData.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
@Tag(name = "지도 API", description = "카카오맵 API 중계 서비스")
public class ApiV1KakaoMapController {

  private final KakaoApiService kakaoApiService;

  @GetMapping("/address")
  @Operation(summary = "좌표를 주소로 변환", description = "위도와 경도를 입력받아 주소로 변환")
  public RsData<KakaoAddressResponse> convertCoordToAddress(
      @RequestParam("longitude") double longitude,
      @RequestParam("latitude") double latitude) {

    KakaoAddressResponse response = kakaoApiService.getAddressFromCoords(longitude, latitude);
    return new RsData<>("200", "좌표를 주소로 변환했습니다.", response);
  }
}
