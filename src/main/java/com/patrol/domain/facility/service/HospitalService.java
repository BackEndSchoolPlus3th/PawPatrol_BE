package com.patrol.domain.facility.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.facility.dto.FacilitiesResponse;
import com.patrol.domain.facility.entity.Hospital;
import com.patrol.domain.facility.repository.HospitalRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HospitalService implements FacilityService {

  private final HospitalRepository hospitalRepository;
  private final CsvParser csvParser;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

  private static final String HOSPITALS_CACHE_KEY = "hospitals:lat:%s:lng:%s:radius:%s";
  private static final long CACHE_TTL_MINUTES = 60;


  @PostConstruct
  @Transactional
  public void loadData() {
    try {
      if (hospitalRepository.count() > 0) {
        log.info("병원 데이터가 이미 존재합니다. 초기 로드를 건너뜁니다.");
        return;
      }

      Resource resource = new ClassPathResource("data/hospitals.csv");
      List<CsvParser.HospitalData> hospitalDataList =
          csvParser.parseHospitals(resource.getInputStream());

      List<Hospital> hospitals = hospitalDataList.stream()
          .map(this::convertToEntity)
          .collect(Collectors.toList());

      hospitalRepository.saveAll(hospitals);
      log.info("병원 데이터 {}개 로드 완료", hospitals.size());

    } catch (Exception e) {
      log.error("병원 데이터 로드 중 에러 발생", e);
    }
  }

  public List<FacilitiesResponse> findAll() {
    return hospitalRepository.findAll().stream()
        .map(FacilitiesResponse::of)
        .collect(Collectors.toList());
  }

  @Override
  public List<FacilitiesResponse> getFacilitiesWithinRadius(
          double latitude, double longitude, double radius
  ) {
    String cacheKey = String.format(HOSPITALS_CACHE_KEY, latitude, longitude, radius);
    String cachedData = redisTemplate.opsForValue().get(cacheKey);
    if (cachedData != null) {
      try {
        log.info("캐싱된 병원 데이터 찾기 성공 : {}", cacheKey);
        return objectMapper.readValue(cachedData, new TypeReference<List<FacilitiesResponse>>() {});
      } catch (JsonProcessingException e) {
        log.error("역직렬화 실패", e);
      }
    }

    log.info("캐싱된 병원 데이터 찾기 실패 : {}", cacheKey);
    List<FacilitiesResponse> hospitals = hospitalRepository.findHospitalsWithinRadius(latitude, longitude, radius)
        .stream()
        .map(FacilitiesResponse::of)
        .collect(Collectors.toList());

    try {
      String hospitalsJson = objectMapper.writeValueAsString(hospitals);
      redisTemplate.opsForValue().set(cacheKey, hospitalsJson, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
      log.info("병원 데이터 캐싱 성공 : {}", cacheKey);
    } catch (JsonProcessingException e) {
      log.error("직렬화 실패", e);
    }

    return hospitals;
  }

  private Hospital convertToEntity(CsvParser.HospitalData data) {
    log.info(data.getTel());

    return Hospital.builder()
        .name(data.getName())
        .address(data.getAddress())
        .tel(data.getTel())
        .latitude(data.getLatitude())
        .longitude(data.getLongitude())
        .operatingHours(data.getOperatingHours())
        .homepage(data.getHomepage())
        .build();
  }
}
