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
  private CaseHistoryStatus historyStatus;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ContentType contentType;

  @Column(nullable = false)
  private Long contentId;

  @Column(nullable = false)
  private Long memberId;

  private LocalDateTime deletedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "animal_case_id", nullable = false)
  private AnimalCase animalCase;

}
