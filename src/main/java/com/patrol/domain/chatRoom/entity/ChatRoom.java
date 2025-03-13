package com.patrol.domain.chatRoom.entity;

import com.patrol.domain.Postable.Postable;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter
@Setter
@Entity
@NoArgsConstructor
@SuperBuilder
public class ChatRoom extends BaseEntity {
    private ChatRoomType type;

    @ManyToOne
    @JoinColumn(name = "lost_found_post_id")
    private LostFoundPost lostFoundPost;

    @ManyToOne
    @JoinColumn(name = "animal_case_id")
    private AnimalCase animalCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member1_id")
    private Member member1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member2_id")
    private Member member2;

    private String roomIdentifier;

    public static String createRoomIdentifier(Postable post, Member member1, Member member2, ChatRoomType type) {
        Long postId = post.getId();
        Long smallerId = Math.min(member1.getId(), member2.getId());
        Long largerId = Math.max(member1.getId(), member2.getId());

        return String.format("%d_%d_%d_%s", postId, smallerId, largerId, type.name());
    }

    public Object getPost() {
        if (type == ChatRoomType.LOSTFOUND) {
            return lostFoundPost;
        } else {
            return animalCase;
        }
    }

    public Long getPostId() {
        if (type == ChatRoomType.LOSTFOUND && lostFoundPost != null) {
            return lostFoundPost.getId();
        } else if (type != ChatRoomType.LOSTFOUND && animalCase != null) {
            return animalCase.getId();
        }
        return null;
    }
}
