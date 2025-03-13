package com.patrol.domain.comment.service;

import com.patrol.domain.comment.entity.Comment;
import com.patrol.domain.comment.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publishCommentCreated(Comment comment) {
        CommentCreatedEvent event = new CommentCreatedEvent(this, comment);
        eventPublisher.publishEvent(event);
    }
}