package com.patrol.domain.facility.repository;


import com.patrol.domain.facility.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

import java.util.Collection;


public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    @Query(value = "SELECT h FROM Hospital h " +
            "WHERE (6371 * acos(cos(radians(:latitude)) * cos(radians(h.latitude)) * " +
            "cos(radians(h.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
            "sin(radians(h.latitude)))) <= :radius")
    List<Hospital> findHospitalsWithinRadius(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radius") double radius
    );

}
