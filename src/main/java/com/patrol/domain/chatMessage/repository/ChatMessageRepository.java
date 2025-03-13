package com.patrol.domain.chatMessage.repository;

import com.patrol.domain.chatMessage.entity.ChatMessage;
import com.patrol.domain.chatRoom.entity.ChatRoom;
import com.patrol.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository< ChatMessage, Long> {
    List<ChatMessage> findAllByChatRoom(ChatRoom chatRoom);

    List<ChatMessage> findAllByChatRoomAndReceiverAndIsReadFalse(ChatRoom chatRoom, Member loginUser);

    @Query("SELECT m FROM ChatMessage m " +
            "WHERE m.id = (SELECT MAX(m2.id) FROM ChatMessage m2 WHERE m2.chatRoom = :chatRoom)")
    ChatMessage findLatestMessageForChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    @Query("SELECT m.chatRoom.id as chatRoomId, COUNT(m) as unreadCount " +
            "FROM ChatMessage m " +
            "WHERE m.chatRoom IN :chatRooms AND m.receiver.id = :userId AND m.isRead = false " +
            "GROUP BY m.chatRoom.id")
    List<Object[]> countUnreadMessagesForChatRoomsByReceiverId(
            @Param("chatRooms") List<ChatRoom> chatRooms,
            @Param("userId") Long userId
    );
}
