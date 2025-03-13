package com.patrol.domain.chatMessage.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.chatMessage.dto.RequestMessage;
import com.patrol.api.chatMessage.dto.ResponseMessage;
import com.patrol.api.image.dto.ImageResponseDto;
import com.patrol.api.member.member.dto.MemberResponseDto;
import com.patrol.domain.Postable.Postable;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.repository.AnimalCaseRepository;
import com.patrol.domain.chatMessage.entity.ChatMessage;
import com.patrol.domain.chatMessage.entity.MessageType;
import com.patrol.domain.chatMessage.repository.ChatMessageRepository;
import com.patrol.domain.chatRoom.entity.ChatRoom;
import com.patrol.domain.chatRoom.entity.ChatRoomType;
import com.patrol.domain.chatRoom.repository.ChatRoomRepository;
import com.patrol.domain.lostFoundPost.entity.LostFoundPost;
import com.patrol.domain.lostFoundPost.repository.LostFoundPostRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.repository.MemberRepository;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import com.patrol.global.jpa.BaseEntity;
import com.patrol.global.rsData.RsData;
import com.patrol.global.storage.FileStorageHandler;
import com.patrol.global.storage.FileUploadRequest;
import com.patrol.global.storage.FileUploadResult;
import com.patrol.global.storage.NcpObjectStorageService;
import com.patrol.global.webSocket.WebSocketEventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.*;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final LostFoundPostRepository lostFoundPostRepository;
    private final MemberRepository memberRepository;
    private final AnimalCaseRepository animalCaseRepository;
    private final FileStorageHandler fileStorageHandler;
    private final ObjectMapper objectMapper;
    private final NcpObjectStorageService ncpObjectStorageService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebSocketEventListener webSocketEventListener;

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

    @Transactional
    public RsData<Object> writeMessage(Long postId, RequestMessage requestMessage, ChatRoomType type) {
        try {
            // 입력값 유효성 검사
            if (requestMessage.getContent() == null || requestMessage.getContent().trim().isEmpty()) {
                return new RsData<>("400", "채팅 메시지는 비어 있을 수 없습니다.");
            }
            if (requestMessage.getContent().length() > 250) {
                return new RsData<>("400", "채팅 메시지는 250자를 넘을 수 없습니다.");
            }

            // 게시물 타입에 따라 처리 - Object 타입으로 변경
            Object post;

            if (type == ChatRoomType.LOSTFOUND) {
                Optional<LostFoundPost> postOptional = lostFoundPostRepository.findById(postId);

                if (postOptional.isEmpty()) {
                    return new RsData<>("404", "해당 게시물을 찾을 수 없습니다.");
                }

                post = postOptional.get();
            } else {
                Optional<AnimalCase> postOptional = animalCaseRepository.findById(postId);
                if (postOptional.isEmpty()) {
                    return new RsData<>("404", "해당 게시물을 찾을 수 없습니다.");
                }

                post = postOptional.get();
            }

            // 송신자 및 수신자 확인
            Member receiver;
            Member sender;

            if (requestMessage.getReceiverId() != null && requestMessage.getSenderId() != null) {
                Optional<Member> receiverOptional = memberRepository.findById(requestMessage.getReceiverId());
                Optional<Member> senderOptional = memberRepository.findById(requestMessage.getSenderId());

                if (receiverOptional.isEmpty()) {
                    return new RsData<>("404", "수신자를 찾을 수 없습니다.");
                }

                if (senderOptional.isEmpty()) {
                    return new RsData<>("404", "발신자를 찾을 수 없습니다.");
                }

                sender = senderOptional.get();
                receiver = receiverOptional.get();
            } else {
                return new RsData<>("404", "수신자 또는 발신자 정보가 없습니다.");
            }

            // 채팅방 찾기 또는 생성
            ChatRoom chatRoom;
            Optional<ChatRoom> chatRoomOptional;

            // 타입에 따라 다른 메서드 호출
            if (type == ChatRoomType.LOSTFOUND) {
                chatRoomOptional = chatRoomRepository.findByLostFoundPostAndMembers(
                        (LostFoundPost) post, sender, receiver);
            } else {
                chatRoomOptional = chatRoomRepository.findByAnimalCaseAndMembers(
                        (AnimalCase) post, sender, receiver);
            }

            if (chatRoomOptional.isPresent()) {
                chatRoom = chatRoomOptional.get();
            } else {
                // 새 채팅방 생성
                String roomIdentifier;

                if (type == ChatRoomType.LOSTFOUND) {
                    roomIdentifier = ChatRoom.createRoomIdentifier(
                            (LostFoundPost) post, sender, receiver, type);

                    chatRoom = ChatRoom.builder()
                            .lostFoundPost((LostFoundPost) post)
                            .type(type)
                            .member1(sender)
                            .member2(receiver)
                            .roomIdentifier(roomIdentifier)
                            .build();
                } else {
                    roomIdentifier = ChatRoom.createRoomIdentifier(
                            (Postable) post, sender, receiver, type);

                    chatRoom = ChatRoom.builder()
                            .animalCase((AnimalCase) post)
                            .type(type)
                            .member1(sender)
                            .member2(receiver)
                            .roomIdentifier(roomIdentifier)
                            .build();
                }

                chatRoomRepository.save(chatRoom);
                logger.info("New chat room created: {}", roomIdentifier);
            }

            // 채팅 메시지 생성 및 저장
            ChatMessage chatMessage = ChatMessage.builder()
                    .content(requestMessage.getContent())
                    .sender(sender)
                    .receiver(receiver)
                    .chatRoom(chatRoom)
                    .isRead(false)
                    .build();
            chatMessageRepository.save(chatMessage);

            // 응답 메시지 DTO 생성
            Long postIdValue;
            if (post instanceof LostFoundPost) {
                postIdValue = ((LostFoundPost) post).getId();
            } else {
                postIdValue = ((AnimalCase) post).getId();
            }

            ResponseMessage messageDTO = ResponseMessage.builder()
                    .id(chatMessage.getId())
                    .content(chatMessage.getContent())
                    .sender(new MemberResponseDto(sender))
                    .receiver(new MemberResponseDto(receiver))
                    .postId(postIdValue)
                    .timestamp(chatMessage.getCreatedAt())
                    .isRead(chatMessage.isRead())
                    .messageType(chatMessage.getMessageType())
                    .roomIdentifier(chatRoom.getRoomIdentifier())
                    .build();

            // 메시지 전송 (실시간 또는 오프라인)
            String receiverId = String.valueOf(receiver.getId());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            String messageJson = objectMapper.writeValueAsString(messageDTO);

            if (webSocketEventListener.isSubscribedToRoom(receiverId, chatRoom.getRoomIdentifier())) {
                logger.info("실시간 채팅으로 메시지 전송");
                kafkaTemplate.send("real-time-chat-messages", messageJson);
            } else {
                logger.info("오프라인 알림으로 메시지 전송");
                kafkaTemplate.send("offline-notifications", messageJson);
            }

            return new RsData<>("200", "채팅 메시지 작성 성공", chatRoom.getId());
        } catch (Exception e) {
            logger.error("Error sending message: ", e);
            return new RsData<>("500", "서버 내부 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional
    public RsData<Long> createImage(String roomIdentifier, List<MultipartFile> images, Member loginUser) {
        try {
            if (images == null || images.isEmpty()) {
                return new RsData<>("400", "이미지가 필요합니다.", null);
            }
            ChatRoom chatRoom;
            Member receiver;
            Object post; // Postable 인터페이스 대신 Object 타입 사용

            Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByRoomIdentifier(roomIdentifier);

            if (chatRoomOptional.isPresent()) {
                chatRoom = chatRoomOptional.get();
                if (chatRoom.getMember1().getId().equals(loginUser.getId())) {
                    receiver = chatRoom.getMember2();
                } else {
                    receiver = chatRoom.getMember1();
                }
                post = chatRoom.getPost(); // getPost() 도우미 메서드 사용
            } else {
                String[] parts = roomIdentifier.split("_");
                if (parts.length != 4) { // LOSTFOUND_postId_memberId1_memberId2 형식이므로 4개로 분리됨
                    return new RsData<>("400", "잘못된 채팅방 식별자 형식입니다.", null);
                }

                ChatRoomType type = ChatRoomType.valueOf(parts[0]);
                Long postId = Long.parseLong(parts[1]);
                Long memberId1 = Long.parseLong(parts[2]);
                Long memberId2 = Long.parseLong(parts[3]);

                Long receiverId = (loginUser.getId().equals(memberId1)) ? memberId2 : memberId1;

                // 게시물 타입에 따라 처리
                if (type == ChatRoomType.LOSTFOUND) {
                    Optional<LostFoundPost> postOptional = lostFoundPostRepository.findById(postId);
                    if (postOptional.isEmpty()) {
                        return new RsData<>("404", "해당 게시물을 찾을 수 없습니다.", null);
                    }
                    post = postOptional.get();
                } else {
                    Optional<AnimalCase> postOptional = animalCaseRepository.findById(postId);
                    if (postOptional.isEmpty()) {
                        return new RsData<>("404", "해당 게시물을 찾을 수 없습니다.", null);
                    }
                    post = postOptional.get();
                }

                Optional<Member> receiverOptional = memberRepository.findById(receiverId);
                if (receiverOptional.isEmpty()) {
                    return new RsData<>("404", "수신자를 찾을 수 없습니다.", null);
                }
                receiver = receiverOptional.get();

                // 채팅방 타입에 따라 다른 빌더 메서드 사용
                if (type == ChatRoomType.LOSTFOUND) {
                    chatRoom = ChatRoom.builder()
                            .lostFoundPost((LostFoundPost) post)
                            .type(type)
                            .member1(loginUser)
                            .member2(receiver)
                            .roomIdentifier(roomIdentifier)
                            .build();
                } else {
                    chatRoom = ChatRoom.builder()
                            .animalCase((AnimalCase) post)
                            .type(type)
                            .member1(loginUser)
                            .member2(receiver)
                            .roomIdentifier(roomIdentifier)
                            .build();
                }

                chatRoomRepository.save(chatRoom);
            }

            List<String> uploadedPaths = new ArrayList<>();
            List<ImageResponseDto> imageDtos = new ArrayList<>();

            try {
                for (MultipartFile image : images) {
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentLength(image.getSize());
                    metadata.setContentType(image.getContentType());

                    FileUploadResult uploadResult = fileStorageHandler.handleFileUpload(
                            FileUploadRequest.builder()
                                    .folderPath("chat/images/")
                                    .file(image)
                                    .build()
                    );

                    if (uploadResult != null) {
                        String fullPath = uploadResult.getFullPath();
                        uploadedPaths.add(fullPath);

                        ImageResponseDto imageDto = new ImageResponseDto(
                                fullPath
                        );
                        imageDtos.add(imageDto);
                    }
                }

                // 이미지 정보를 JSON으로 변환
                String imageContent = objectMapper.writeValueAsString(imageDtos);

                // 채팅 메시지 생성
                ChatMessage chatMessage = ChatMessage.builder()
                        .content(imageContent)
                        .messageType(MessageType.IMAGE)
                        .sender(loginUser)
                        .receiver(receiver)
                        .chatRoom(chatRoom)
                        .isRead(false)
                        .build();

                chatMessageRepository.save(chatMessage);

                // 게시물 ID 가져오기
                Long postId = chatRoom.getPostId(); // 도우미 메서드 사용

                // 응답 메시지 생성
                ResponseMessage messageDTO = ResponseMessage.builder()
                        .id(chatMessage.getId())
                        .content(imageContent)
                        .messageType(MessageType.IMAGE)
                        .sender(new MemberResponseDto(loginUser))
                        .receiver(new MemberResponseDto(receiver))
                        .postId(postId)
                        .timestamp(chatMessage.getCreatedAt())
                        .isRead(chatMessage.isRead())
                        .roomIdentifier(chatRoom.getRoomIdentifier())
                        .build();

                String receiverId = String.valueOf(receiver.getId());

                if (webSocketEventListener.isSubscribedToRoom(receiverId, chatRoom.getRoomIdentifier())) {
                    logger.info("실시간 채팅으로 메시지 전송");
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    String messageJson = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("real-time-chat-messages", messageJson);
                } else {
                    logger.info("오프라인 알림으로 메시지 전송");
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    String messageJson = objectMapper.writeValueAsString(messageDTO);
                    kafkaTemplate.send("offline-notifications", messageJson);
                }

                return new RsData<>("200", "이미지 메시지 전송 성공", chatRoom.getId());

            } catch (Exception e) {
                for (String path : uploadedPaths) {
                    try {
                        ncpObjectStorageService.delete(path);
                    } catch (Exception deleteError) {
                        logger.error("Failed to delete image after error: {}", deleteError.getMessage());
                    }
                }
                logger.error("Error in creating image message: {}", e.getMessage(), e);
                throw new CustomException(ErrorCode.DATABASE_ERROR);
            }
        } catch (Exception e) {
            logger.error("Unexpected error in createImageMessage: {}", e.getMessage(), e);
            return new RsData<>("500", "서버 오류가 발생했습니다.", null);
        }
    }

    public RsData<Object> getChatMessages(String identifier, Member loginUser) {
        try {
            Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByRoomIdentifier(identifier);
            if (chatRoomOptional.isEmpty()) {
                return new RsData<>("404", "채팅방을 찾을 수 없습니다.");
            }

            ChatRoom chatRoom = chatRoomOptional.get();

            if (!chatRoom.getMember1().getId().equals(loginUser.getId()) &&
                    !chatRoom.getMember2().getId().equals(loginUser.getId())) {
                return new RsData<>("403", "해당 채팅방에 접근 권한이 없습니다.");
            }

            List<ChatMessage> chatMessages = chatMessageRepository.findAllByChatRoom(chatRoom);
            List<ResponseMessage> responseDtos = chatMessages.stream()
                    .map(chatMessage -> ResponseMessage.builder()
                            .id(chatMessage.getId())
                            .content(chatMessage.getContent())
                            .sender(new MemberResponseDto(chatMessage.getSender()))
                            .receiver(new MemberResponseDto(chatMessage.getReceiver()))
                            .postId(chatMessage.getChatRoom().getPostId())
                            .timestamp(chatMessage.getCreatedAt())
                            .isRead(chatMessage.isRead())
                            .messageType(chatMessage.getMessageType())
                            .build())
                    .collect(Collectors.toList());

            return new RsData<>("200", "채팅 메시지 조회 성공", responseDtos);
        } catch (Exception e) {
            logger.error("Error retrieving messages: ", e);
            return new RsData<>("500", "서버 내부 오류가 발생했습니다.");
        }
    }

    @Transactional
    public RsData<Object> markMessagesAsRead(String identifier, Member loginUser) {
        try {
            Optional<ChatRoom> chatRoomOptional = chatRoomRepository.findByRoomIdentifier(identifier);
            if (chatRoomOptional.isEmpty()) {
                return new RsData<>("404", "채팅방을 찾을 수 없습니다.");
            }

            ChatRoom chatRoom = chatRoomOptional.get();

            if (!chatRoom.getMember1().getId().equals(loginUser.getId()) &&
                    !chatRoom.getMember2().getId().equals(loginUser.getId())) {
                return new RsData<>("403", "해당 채팅방에 접근 권한이 없습니다.");
            }

            List<ChatMessage> unreadMessages = chatMessageRepository.findAllByChatRoomAndReceiverAndIsReadFalse(
                    chatRoom, loginUser);

            for (ChatMessage message : unreadMessages) {
                message.setRead(true);
            }

            return new RsData<>("200", "메시지 읽음 처리 완료");
        } catch (Exception e) {
            logger.error("Error marking messages as read: ", e);
            return new RsData<>("500", "서버 내부 오류가 발생했습니다.");
        }
    }
}
