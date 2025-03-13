package com.patrol.domain.chatRoom.repository;

import com.patrol.domain.Postable.Postable;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.chatRoom.entity.ChatRoom;
import com.patrol.domain.chatRoom.entity.ChatRoomType;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.lostFoundPost = :post " +
            "AND ((cr.member1 = :member1 AND cr.member2 = :member2) OR (cr.member1 = :member2 AND cr.member2 = :member1))")
    Optional<ChatRoom> findByLostFoundPostAndMembers(
            @Param("post") LostFoundPost post,
            @Param("member1") Member member1,
            @Param("member2") Member member2);

    // AnimalCase를 위한 메서드
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.animalCase = :post " +
            "AND ((cr.member1 = :member1 AND cr.member2 = :member2) OR (cr.member1 = :member2 AND cr.member2 = :member1))")
    Optional<ChatRoom> findByAnimalCaseAndMembers(
            @Param("post") AnimalCase post,
            @Param("member1") Member member1,
            @Param("member2") Member member2);

    // 룸 식별자로 채팅방 찾기
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomIdentifier = :roomIdentifier")
    Optional<ChatRoom> findByRoomIdentifier(@Param("roomIdentifier") String roomIdentifier);

    @Query("SELECT DISTINCT c FROM ChatRoom c " +
            "LEFT JOIN FETCH c.member1 m1 " +
            "LEFT JOIN FETCH c.member2 m2 " +
            "LEFT JOIN FETCH c.lostFoundPost lfp " +
            "LEFT JOIN FETCH c.animalCase ac " +
            "WHERE m1 = :member OR m2 = :member")
    List<ChatRoom> findAllWithDetailsByMember(@Param("member") Member member);

    // 특정 타입의 채팅방을 회원으로 조회
    @Query("SELECT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.member1 m1 " +
            "LEFT JOIN FETCH cr.member2 m2 " +
            "LEFT JOIN FETCH cr.lostFoundPost lfp " +
            "LEFT JOIN FETCH cr.animalCase ac " +
            "WHERE (cr.member1 = :member OR cr.member2 = :member) " +
            "AND cr.type = :type")
    List<ChatRoom> findAllWithDetailsByMemberAndType(@Param("member") Member member, @Param("type") ChatRoomType type);

}
