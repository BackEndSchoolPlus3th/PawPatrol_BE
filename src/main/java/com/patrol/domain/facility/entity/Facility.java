package com.patrol.domain.facility.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.entity.CaseHistory;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;

@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
public abstract class Facility extends BaseEntity {

  private String name;
  private String address;
  private String tel;
  private Double latitude;
  private Double longitude;
  @Embedded
  private OperatingHours operatingHours;

  private String owner;
  private String businessRegistrationNumber;
}
