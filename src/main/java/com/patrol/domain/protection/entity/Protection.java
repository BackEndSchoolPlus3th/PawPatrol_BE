package com.patrol.domain.protection.entity;

import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.protection.enums.ProtectionStatus;
import com.patrol.domain.protection.enums.ProtectionType;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
@Table(name = "protections")
public class Protection extends BaseEntity {

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProtectionStatus protectionStatus = ProtectionStatus.PENDING;  // 신청 상태 (승인전, 승인, 거절)

  private LocalDateTime approvedDate;  // 신청 승인일
  private String reason;   // 신청 사유
  private String rejectReason;   // 거절 사유
  private LocalDateTime deletedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ProtectionType protectionType;   // TEMP_PROTECTION or ADOPTION

  // N : 1
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "animal_case_id", nullable = false)
  private AnimalCase animalCase;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "applicant_id", nullable = false)
  private Member applicant;

  public void approve() {
    this.protectionStatus = ProtectionStatus.APPROVED;
    this.approvedDate = LocalDateTime.now();
  }

  public void reject(String reason) {
    this.protectionStatus = ProtectionStatus.REJECTED;
    this.rejectReason = reason;
  }

  public void cancel() {
    this.deletedAt = LocalDateTime.now();
  }
}
