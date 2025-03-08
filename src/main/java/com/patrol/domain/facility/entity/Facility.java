package com.patrol.domain.facility.entity;

import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
public abstract class Facility extends BaseEntity {

  private String name; // 시설명
  private String address;
  private String tel;
  private Double latitude;
  private Double longitude;
  @Embedded
  private OperatingHours operatingHours;

  private String owner;   // 대표자
  private String businessRegistrationNumber;  // 사업자 등록번호
}
