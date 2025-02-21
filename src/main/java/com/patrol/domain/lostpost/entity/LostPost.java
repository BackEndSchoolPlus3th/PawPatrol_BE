package com.patrol.domain.lostpost.entity;

import com.patrol.api.lostpost.dto.LostPostRequestDto;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "LostPost")
public class LostPost extends BaseEntity {



    //private Long lostId;
    //private Long foundId;
    @ManyToOne(fetch = FetchType.LAZY)
    private Member author;
    private Long petId;
    private Double latitude;
    private Double longitude;
    private String title;
    private String content;
    private String location;
    private String ownerPhone;

    @Enumerated(EnumType.STRING)
    private Status status; // 상태를 Enum으로 관리

    private String tags;
    private LocalDateTime lostTime;


    // Enum 정의
    public enum Status {
        FINDING("찾는 중"),
        FOSTERING("임보 중"),
        FOUND("주인 찾기 완료");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public LostPost(LostPostRequestDto requestDto) {
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.location = requestDto.getLocation();
        this.ownerPhone = requestDto.getOwnerPhone();
        this.tags = String.join("#", requestDto.getTags());
        this.lostTime = LocalDateTime.parse(requestDto.getLostTime());

        // 상태를 Enum으로 변환하여 저장
        if (requestDto.getStatus() != null) {
            this.status = Status.valueOf(requestDto.getStatus()); // status를 enum으로 변환
        } else {
            this.status = Status.FINDING; // 기본값으로 "찾는 중"을 설정
        }
    }

    public void update(LostPostRequestDto requestDto) {
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.location = requestDto.getLocation();
        this.ownerPhone = requestDto.getOwnerPhone();
        this.tags = String.join("#", requestDto.getTags());
        this.lostTime = LocalDateTime.parse(requestDto.getLostTime());

        // 상태를 Enum으로 변환하여 업데이트
        if (requestDto.getStatus() != null) {
            this.status = Status.valueOf(requestDto.getStatus()); // status를 enum으로 변환
        }
    }

    // ID만을 이용한 생성자 추가
    /*public LostPost(Long lostId) {
        this.lostId = lostId;
    }*/
}
