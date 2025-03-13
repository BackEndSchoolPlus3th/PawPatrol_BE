package com.patrol.domain.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.facility.dto.FacilitiesResponse;
import com.patrol.api.facility.dto.ShelterApiResponse;
import com.patrol.api.facility.dto.ShelterListResponse;
import com.patrol.api.member.auth.dto.SearchShelterResponse;
import com.patrol.domain.facility.entity.OperatingHours;
import com.patrol.domain.facility.entity.QShelter;
import com.patrol.domain.facility.entity.Shelter;
import com.patrol.domain.facility.repository.ShelterRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService implements FacilityService {

  private final ShelterRepository shelterRepository;
  private final ObjectMapper objectMapper;


  @Transactional
  public void saveApiResponse(String jsonResponse) {
    try {
      ShelterApiResponse response = objectMapper.readValue(jsonResponse, ShelterApiResponse.class);

      if (response.getResponse() != null
          && response.getResponse().getBody() != null
          && response.getResponse().getBody().getItems() != null
          && response.getResponse().getBody().getItems().getItem() != null) {

        List<String> existingShelterNames = shelterRepository.findAll().stream()
            .map(Shelter::getName)
            .toList();

        List<Shelter> newShelters = response.getResponse().getBody().getItems().getItem()
            .stream()
            .filter(item -> !existingShelterNames.contains(item.getCareNm()))
            .map(this::convertToEntity)
            .collect(Collectors.toList());

        if (!newShelters.isEmpty()) {
          shelterRepository.saveAll(newShelters);
        }
      }
    } catch (Exception e) {
      log.error("데이터 저장 중 에러 발생: {}", e.getMessage(), e);
      throw new RuntimeException("데이터 저장 실패", e);
    }
  }


  public List<FacilitiesResponse> findAll() {
    return shelterRepository.findAll().stream()
        .map(FacilitiesResponse::of)
        .collect(Collectors.toList());
  }


  public List<ShelterListResponse> findAllWithAnimals() {
    List<Shelter> shelters = shelterRepository.findAllWithAnimalCasesAndAnimals();
    return shelters.stream()
        .map(ShelterListResponse::of)
        .collect(Collectors.toList());
  }

  @Override
  public List<FacilitiesResponse> getFacilitiesWithinRadius(
          double latitude,
          double longitude,
          double radius
  ) {
    return shelterRepository.findSheltersWithinRadius(latitude, longitude, radius)
            .stream()
            .map(FacilitiesResponse::of)
            .collect(Collectors.toList());
  }

  public List<ShelterListResponse> getSheltersWithinRadius(
      double latitude, double longitude, double radius
  ) {
    return shelterRepository.findSheltersWithinRadius(latitude, longitude, radius)
        .stream()
        .map(ShelterListResponse::of)
        .collect(Collectors.toList());
  }


  private Shelter convertToEntity(ShelterApiResponse.Item item) {
    OperatingHours operatingHours = OperatingHours.builder()
        .weekdayTime(formatOperatingHours(item.getWeekOprStime(), item.getWeekOprEtime()))
        .weekendTime(formatOperatingHours(item.getWeekendOprStime(), item.getWeekendOprEtime()))
        .closedDays(item.getCloseDay())
        .build();

    return Shelter.builder()
        .name(item.getCareNm())
        .address(item.getCareAddr())
        .tel(item.getCareTel())
        .latitude(item.getLat())
        .longitude(item.getLng())
        .operatingHours(operatingHours)
        .vetPersonCount(item.getVetPersonCnt())
        .saveTargetAnimal(item.getSaveTrgtAnimal())
        .build();
  }

  private String formatOperatingHours(String startTime, String endTime) {
    if (startTime == null || endTime == null || startTime.isEmpty() || endTime.isEmpty()) {
      return null;
    }
    return startTime + " - " + endTime;
  }

  public Page<ShelterListResponse> getAllShelter(Pageable pageable) {
    Page<Shelter> shelterPage = shelterRepository.findAllWithAnimalCasesAndAnimals(pageable);
    return shelterPage.map(ShelterListResponse::of);
  }

  public List<SearchShelterResponse> searchShelters(String keyword) {
    QShelter qShelter = QShelter.shelter;
    BooleanBuilder builder = new BooleanBuilder();

    if(keyword != null && !keyword.isEmpty()) {
      builder.and(qShelter.name.containsIgnoreCase(keyword));
    }

    Iterable<Shelter> shelterIterable = shelterRepository.findAll(builder);
    List<Shelter> shelterList = new ArrayList<>();

    shelterIterable.forEach(shelterList::add);
    return shelterList.stream()
            .map(shelter -> SearchShelterResponse.builder()
                    .id(shelter.getId())
                    .name(shelter.getName())
                    .address(shelter.getAddress())
                    .tel(shelter.getTel())
                    .build())
            .collect(Collectors.toList());
  }
}
