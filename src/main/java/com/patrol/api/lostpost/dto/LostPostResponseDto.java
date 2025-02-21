package com.patrol.api.lostpost.dto;

import com.patrol.domain.lostpost.entity.LostPost;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LostPostResponseDto {
    //private Long lostId;
    private String status;
    private String title;       // 제목
    private String content;     // 내용
    private String tags;  // 태그를 String으로 저장 (구분자로 연결된 문자열)
    private String location;    // 위치
    private String ownerPhone;  // 보호자 전화번호
    private String lostTime;    // 실종 시간
    private String nickname;  // ✅ 추가된 필드 (작성자 닉네임)

    public LostPostResponseDto(LostPost lostPost) {
        //this.lostId = lostPost.getLostId();
        // status가 Enum일 때 getDescription()을 사용하여 String 값을 가져옴
        this.nickname = lostPost.getAuthor().getNickname();  // ✅ Member에서 nickname 가져오기
        this.status = lostPost.getStatus().getDescription();
        this.title = lostPost.getTitle();
        this.content = lostPost.getContent();
        this.tags = lostPost.getTags();
        this.location = lostPost.getLocation();
        this.ownerPhone = lostPost.getOwnerPhone();
        this.lostTime = lostPost.getLostTime().toString();  // LocalDateTime을 String으로 변환
    }
}

