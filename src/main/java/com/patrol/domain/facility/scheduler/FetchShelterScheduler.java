package com.patrol.domain.facility.scheduler;

import com.patrol.domain.facility.service.ShelterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class FetchShelterScheduler {

  @Value("${schedule.use}")
  private boolean useSchedule;

  @Value("${apis.shelter.service-key}")
  private String serviceKeyForShelter;

  @Value("${apis.shelter.url}")
  private String apiUrl;

  private final ShelterService shelterService;

  @PostConstruct
  public void getApisApiData() {

    if(useSchedule) {
      try {
        log.info("동물보호센터 API 스케줄러 실행");
        String jsonResponse = fetchShelterData();
        shelterService.saveApiResponse(jsonResponse);

      } catch (Exception e) {
        log.error("동물보호센터 API 호출 에러 발생", e);

      } finally {
        log.info("동물보호센터 API 스케줄러 종료");
      }
    }
  }



  private String fetchShelterData() {
    try {
      String url = String.format("%s?serviceKey=%s&numOfRows=1000&pageNo=1&_type=json",
          apiUrl, serviceKeyForShelter
      );
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


}
