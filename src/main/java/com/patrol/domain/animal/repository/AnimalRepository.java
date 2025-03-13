package com.patrol.domain.animal.repository;

import com.patrol.domain.animal.entity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * packageName    : com.patrol.domain.animal.repository
 * fileName       : AnimalRepository
 * author         : sungjun
 * date           : 2025-02-24
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-24        kyd54       최초 생성
 */
@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Page<Animal> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<Animal> findByRegistrationNo(String animalNo);
}
