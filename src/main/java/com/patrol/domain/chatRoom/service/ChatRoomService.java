package com.patrol.domain.chatRoom.service;

import com.patrol.api.chatMessage.dto.ResponseMessage;
import com.patrol.api.chatRoom.dto.ChatRoomResponseDto;
import com.patrol.api.member.member.dto.MemberResponseDto;
import com.patrol.domain.chatMessage.entity.ChatMessage;
import com.patrol.domain.chatMessage.repository.ChatMessageRepository;
import com.patrol.domain.chatRoom.entity.ChatRoom;
import com.patrol.domain.chatRoom.entity.ChatRoomType;
import com.patrol.domain.chatRoom.repository.ChatRoomRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.rsData.RsData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public RsData<Object> getUserChatRooms(Member loginUser) {
        try {
            List<ChatRoom> chatRooms = chatRoomRepository.findAllWithDetailsByMember(loginUser);
            return processChatRooms(chatRooms, loginUser);
        } catch (Exception e) {
            System.out.println(e);
            return new RsData<>("500", "채팅방 목록 조회 실패");
        }
    }

    public RsData<Object> getUserChatRoomsByType(Member loginUser, ChatRoomType type) {
        try {
            List<ChatRoom> chatRooms = chatRoomRepository.findAllWithDetailsByMemberAndType(loginUser, type);
            return processChatRooms(chatRooms, loginUser);
        } catch (Exception e) {
            System.out.println(e);
            return new RsData<>("500", "채팅방 목록 조회 실패");
        }
    }

    private RsData<Object> processChatRooms(List<ChatRoom> chatRooms, Member loginUser) {
        List<ChatMessage> latestMessages = findLatestMessages(chatRooms);

        Map<Long, ChatMessage> roomToLatestMessage = latestMessages.stream()
                .collect(Collectors.toMap(
                        message -> message.getChatRoom().getId(),
                        message -> message
                ));

        List<Object[]> unreadCountsResult = chatMessageRepository.countUnreadMessagesForChatRoomsByReceiverId(
                chatRooms, loginUser.getId());

        Map<Long, Integer> roomToUnreadCount = new HashMap<>();
        for (Object[] result : unreadCountsResult) {
            Long roomId = (Long) result[0];
            Long count = (Long) result[1];
            roomToUnreadCount.put(roomId, count.intValue());
        }

        List<ChatRoomResponseDto> chatRoomDtos = chatRooms.stream()
                .map(room -> {
                    ChatMessage latestMessage = roomToLatestMessage.get(room.getId());
                    ResponseMessage messageDto = null;
                    if (latestMessage != null) {
                        messageDto = ResponseMessage.builder()
                                .id(latestMessage.getId())
                                .content(latestMessage.getContent())
                                .sender(new MemberResponseDto(latestMessage.getSender()))
                                .receiver(new MemberResponseDto(latestMessage.getReceiver()))
                                .postId(room.getPostId())
                                .timestamp(latestMessage.getCreatedAt())
                                .isRead(latestMessage.isRead())
                                .messageType(latestMessage.getMessageType())
                                .build();
                    }
                    int unreadCount = roomToUnreadCount.getOrDefault(room.getId(), 0);
                    return new ChatRoomResponseDto(room, messageDto, unreadCount);
                })
                .collect(Collectors.toList());

        return new RsData<>("200", "채팅방 목록 조회 성공", chatRoomDtos);
    }

    public List<ChatMessage> findLatestMessages(List<ChatRoom> chatRooms) {
        return chatRooms.stream()
                .map(chatRoom -> chatMessageRepository.findLatestMessageForChatRoom(chatRoom))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
