package com.patrol.domain.comment.event;

import com.patrol.domain.comment.entity.Comment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CommentCreatedEvent extends ApplicationEvent {

    private final Comment comment;

    public CommentCreatedEvent(Object source, Comment comment) {
        super(source);
        this.comment = comment;
    }
}