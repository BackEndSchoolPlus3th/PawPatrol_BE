package com.patrol.domain.comment.repository;

import com.patrol.domain.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByLostFoundPostId(Long findPostId);
}
