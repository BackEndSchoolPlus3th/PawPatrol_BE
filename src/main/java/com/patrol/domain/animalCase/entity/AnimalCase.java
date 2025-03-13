package com.patrol.domain.animalCase.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patrol.domain.Postable.Postable;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.facility.entity.Shelter;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.protection.entity.Protection;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "animal_cases")
public class AnimalCase extends BaseEntity implements Postable {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CaseStatus status;  // 상태 : 실종, 발견, 구조, 임시보호, 완료 등

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "animal_id")
  private Animal animal;

  private String title;
  private String description;
  private String location;
  private LocalDateTime deletedAt;

  @JsonIgnore
  @OneToMany(mappedBy = "animalCase", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<CaseHistory> histories = new ArrayList<>();

  public void addHistory(CaseHistory caseHistory) {
    histories.add(caseHistory);
  }

  public void updateStatus(CaseStatus status) {
    this.status = status;
  }

  @JsonIgnore
  @OneToMany(mappedBy = "animalCase", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Protection> protections = new ArrayList<>();


  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_foster_id")
  private Member currentFoster;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shelter_id")
  private Shelter shelter;

  @Override
  public Long getId() {
    return super.getId();
  }

}
