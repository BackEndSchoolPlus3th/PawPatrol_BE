package com.patrol.domain.facility.repository;

import com.patrol.domain.facility.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ShelterRepository extends JpaRepository<Shelter, Long> {
    @Query("SELECT DISTINCT s FROM Shelter s " +
        "LEFT JOIN FETCH s.animalCases ac " +
        "LEFT JOIN FETCH ac.animal " +
        "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * " +
        "cos(radians(s.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
        "sin(radians(s.latitude)))) <= :radius")
    List<Shelter> findSheltersWithinRadius(
        @Param("latitude") double latitude,
        @Param("longitude") double longitude,
        @Param("radius") double radius
    );

    @Query("SELECT s FROM Shelter s " +
        "LEFT JOIN FETCH s.animalCases ac " +
        "LEFT JOIN FETCH ac.animal")
    List<Shelter> findAllWithAnimalCasesAndAnimals();

    Optional<Shelter> findByName(String centerName);
}


