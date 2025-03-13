package com.patrol.domain.animal.repository;

import com.patrol.domain.animal.entity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, Long> {
    Page<Animal> findByOwnerId(Long ownerId, Pageable pageable);

    Optional<Animal> findByRegistrationNo(String animalNo);
}
