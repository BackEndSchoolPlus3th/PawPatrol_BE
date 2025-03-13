package com.patrol.domain.animalCase.entity;

import com.patrol.domain.animalCase.enums.CaseHistoryStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "case_histories")
public class CaseHistory extends BaseEntity {

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CaseHistoryStatus historyStatus;  // 행동 하나하나의 기록

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContentType contentType;  // 기록 대상 : findPost, lostPost, protection, animalCase

  @Column(nullable = false)
  private Long contentId;  // 기록 대상의 ID

  @Column(nullable = false)
  private Long memberId;  // 행위 주체자 ID

  private LocalDateTime deletedAt;

  // 1 : N
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "animal_case_id", nullable = false)
  private AnimalCase animalCase;

}
