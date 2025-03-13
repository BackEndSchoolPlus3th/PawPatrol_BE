package com.patrol.domain.animalCase.events;

import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animalCase.enums.CaseHistoryStatus;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.member.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AnimalCaseCreatedEvent {
  private final Member member;
  private final Animal animal;
  private final String title;
  private final String description;
  private final String location;
  private final CaseStatus toStatus;
}
