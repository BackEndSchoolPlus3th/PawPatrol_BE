package com.patrol.domain.lostpost.repository;
import com.patrol.domain.lostpost.entity.LostPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LostPostRepository extends JpaRepository<LostPost, Long> {
}
