package com.patrol.domain.facility.scheduler;

import com.patrol.domain.facility.service.AdoptionAnimalService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class FetchAdoptionAnimalScheduler {

  @Value("${schedule.use}")
  private boolean useSchedule;

  @Value("${apis.adoption.api-key}")
  private String apiKey;

  @Value("${apis.adoption.url}")
  private String apiUrl;

  @Value("${apis.adoptionimage.api-key}")
  private String imageApiKey;

  @Value("${apis.adoptionimage.url}")
  private String imageApiUrl;


  private final AdoptionAnimalService adoptionAnimalService;

  @Scheduled(cron = "${schedule.cron_for_adoption_animals}")
  @PostConstruct
  public void fetchAdoptionAnimals() {
    if (useSchedule) {
      try {
        log.info("입양대기동물 API 스케줄러 실행");
        String jsonResponse = fetchAdoptionAnimalsData();
        String jsonImageResponse = fetchAdoptionAnimalImagesData();
        adoptionAnimalService.saveApiResponse(jsonResponse, jsonImageResponse);

      } catch (Exception e) {
        log.error("입양대기동물 API 호출 에러 발생", e);

      } finally {
        log.info("입양대기동물 API 스케줄러 종료");
      }
    }
  }

  private String fetchAdoptionAnimalsData() {
    try {
      // TbAdpWaitAnimalView는 서비스명, 1은 시작위치, 1000은 종료위치
      String url = String.format("%s/%s/json/TbAdpWaitAnimalView/1/1000",
          apiUrl, apiKey);
      log.info("API Request URL: {}", url);

      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");

      StringBuilder response = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
        String line;
        while ((line = reader.readLine()) != null) {
          response.append(line);
        }
      }

      return response.toString();
    } catch (Exception e) {
      log.error("API 호출 중 에러 발생", e);
      throw new RuntimeException("API 호출 실패", e);
    }
  }

  private String fetchAdoptionAnimalImagesData() {
    try {
      String url = String.format("%s/%s/json/TbAdpWaitAnimalPhotoView/1/1000",
          imageApiUrl, imageApiKey);
      log.info("이미지 API Request URL: {}", url);

      HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Accept", "application/json");

      StringBuilder response = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(
          new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
        String line;
        while ((line = reader.readLine()) != null) {
          response.append(line);
        }
      }

      return response.toString();
    } catch (Exception e) {
      log.error("이미지 API 호출 중 에러 발생", e);
      throw new RuntimeException("이미지 API 호출 실패", e);
    }
  }
}
