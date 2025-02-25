package com.patrol.domain.animalCase.events;


import com.patrol.domain.animalCase.enums.ContentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostCreatedEvent{
  private final ContentType contentType;      // FINDPOST, LOSTPOST
  private final Long lostFoundPostId;     // lostFoundPost의 ID
  private final Long animalId;
  private final Long memberId;
}
