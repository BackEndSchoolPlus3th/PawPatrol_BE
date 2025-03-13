package com.patrol.api.chatRoom.dto;

import com.patrol.api.PostResponseDto.PostResponseDto;
import com.patrol.api.animalCase.dto.AnimalCaseResponseDto;
import com.patrol.api.chatMessage.dto.ResponseMessage;
import com.patrol.api.lostFoundPost.dto.LostFoundPostResponseDto;
import com.patrol.api.member.member.dto.MemberResponseDto;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.chatRoom.entity.ChatRoom;
import com.patrol.domain.chatRoom.entity.ChatRoomType;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomResponseDto {
    private Long id;
    private String roomIdentifier;
    private MemberResponseDto member1;
    private MemberResponseDto member2;
    private PostResponseDto post;
    private ResponseMessage lastMessage;
    private int unreadCount;
    private ChatRoomType type;

    public ChatRoomResponseDto(ChatRoom chatRoom, ResponseMessage lastMessage, int unreadCount) {
        this.id = chatRoom.getId();
        this.roomIdentifier = chatRoom.getRoomIdentifier();
        this.member1 = new MemberResponseDto(chatRoom.getMember1());
        this.member2 = new MemberResponseDto(chatRoom.getMember2());

        ChatRoomType roomType = chatRoom.getType();

        if (roomType == ChatRoomType.LOSTFOUND) {
            LostFoundPost lostFoundPost = chatRoom.getLostFoundPost();
            this.post = new LostFoundPostResponseDto(lostFoundPost);
        } else {
            AnimalCase animalCase = chatRoom.getAnimalCase();
            this.post = new AnimalCaseResponseDto(animalCase);
        }

        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.type=chatRoom.getType();
    }
}
