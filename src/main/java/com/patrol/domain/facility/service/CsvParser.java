package com.patrol.domain.facility.service;

import com.patrol.domain.facility.entity.OperatingHours;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CsvParser {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HospitalData {
    private String name;
    private String address;
    private String tel;
    private Double latitude;
    private Double longitude;
    private OperatingHours operatingHours;
    private String homepage;
  }


  public List<HospitalData> parseHospitals(InputStream inputStream) {
    List<HospitalData> hospitalDataList = new ArrayList<>();

    try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        CSVParser csvParser = new CSVParser(br, CSVFormat.DEFAULT
            .withHeader("FRNM_NM", "RN_ADDR", "OPR_TIME_INFO", "RPRS_TELNO", "HMPG_URL", "LA_VLUE", "LO_VLUE")
            .withTrim()
            .withEscape('\\')
            .withQuote('"')
            .withIgnoreSurroundingSpaces()
            .withIgnoreEmptyLines())) {

      for (CSVRecord record : csvParser) {
        try {
          HospitalData hospitalData = HospitalData.builder()
              .name(record.get("FRNM_NM"))
              .address(record.get("RN_ADDR"))
              .tel(record.get("RPRS_TELNO"))
              .latitude(parseDouble(record.get("LA_VLUE")))
              .longitude(parseDouble(record.get("LO_VLUE")))
              .homepage(record.get("HMPG_URL"))
              .operatingHours(parseOperatingHours(record.get("OPR_TIME_INFO")))
              .build();

          hospitalDataList.add(hospitalData);

        } catch (IllegalArgumentException e) { // 헤더 불일치 예외 처리
          log.error("CSV 헤더가 일치하지 않습니다. {}", e.getMessage());
          throw new RuntimeException("CSV 헤더가 일치하지 않습니다.", e);
        } catch (Exception e) {
          log.error("라인 파싱 중 에러 발생: {}", record, e);
        }
      }
    } catch (IOException e) {
      log.error("CSV 파일 읽기 중 에러 발생", e);
      throw new RuntimeException("CSV 파일 읽기 실패", e);
    }

    return hospitalDataList;
  }


  private Double parseDouble(String value) {
    try {
      return Double.parseDouble(value.trim());
    } catch (Exception e) {
      return null;
    }
  }

  private OperatingHours parseOperatingHours(String rawData) {
    if (rawData == null || rawData.trim().isEmpty()) {
      return null;
    }

    try {
      if (rawData.contains("매일 00:00 - 24:00")) {
        return OperatingHours.builder()
            .weekdayTime("00:00 - 24:00")
            .weekendTime("00:00 - 24:00")
            .build();
      }

      String[] parts = rawData.split("\\|");

      String weekdayTime = null;
      if (parts.length > 0) {
        weekdayTime = extractDayTime(parts[0]);
      }

      String weekendTime = null;
      for (String part : parts) {
        part = part.trim();
        if (part.startsWith("토")) {
          weekendTime = extractDayTime(part);
          break;
        }
      }

      String closedDays = null;
      for (String part : parts) {
        part = part.trim();
        if (part.contains("휴무")) {
          closedDays = extractClosedDays(part);
          break;
        }
      }

      return OperatingHours.builder()
          .weekdayTime(weekdayTime)
          .weekendTime(weekendTime)
          .closedDays(closedDays)
          .build();

    } catch (Exception e) {
      log.error("운영시간 파싱 중 에러 발생: {}", rawData, e);
      return null;
    }
  }

  private String extractDayTime(String dayData) {
    try {
      String[] parts = dayData.trim().split(" ", 2);
      return parts.length > 1 ? parts[1].trim() : null;
    } catch (Exception e) {
      return null;
    }
  }

  private String extractClosedDays(String closedData) {
    try {
      if (closedData.contains("(") && closedData.contains(")")) {
        int start = closedData.indexOf("(") + 1;
        int end = closedData.indexOf(")");
        return closedData.substring(start, end).trim();
      }

      if (closedData.contains("정기휴무")) {
        return "매주 " + closedData.split(" ")[0] + "요일";
      }

      return closedData.trim();
    } catch (Exception e) {
      return null;
    }
  }
}
