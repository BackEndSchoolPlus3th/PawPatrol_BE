package com.patrol.domain.comment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patrol.domain.lostpost.entity.LostPost;
import com.patrol.domain.lostfound.entity.FindPost;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Comment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {


    @Column(name = "content")
    private String content;

    @JsonIgnore
    @ManyToOne
    private Member author;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_id")
    private LostPost lostPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_id")
    private FindPost findPost;  // ✅ 올바른 변수명

}

