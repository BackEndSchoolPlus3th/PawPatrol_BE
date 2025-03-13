package com.patrol.domain.animalCase.repository;

import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.member.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface AnimalCaseRepository extends JpaRepository<AnimalCase, Long> {
  @Query("SELECT ac FROM AnimalCase ac LEFT JOIN FETCH ac.animal WHERE ac.animal = :animal AND ac.deletedAt IS NULL")
  AnimalCase findByAnimal(@Param("animal") Animal animal);

  @Query("SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "WHERE ac.id = :id AND ac.status = :status AND ac.deletedAt IS NULL")
  Optional<AnimalCase> findByIdAndStatus(
      @Param("id") Long id,
      @Param("status") CaseStatus caseStatus
  );

  // 관리자용 - deletedAt 조건 없음
  @Query("SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "LEFT JOIN FETCH ac.histories " +
      "WHERE ac.id = :id")
  Optional<AnimalCase> findByIdWithHistories(@Param("id") Long id);

  @Query("SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "LEFT JOIN FETCH ac.histories " +
      "WHERE ac.id = :id AND ac.status IN :statuses AND ac.deletedAt IS NULL")
  Optional<AnimalCase> findByIdAndStatusesWithHistories(
      @Param("id") Long id,
      @Param("statuses") Collection<CaseStatus> statuses
  );

  @Query(value = "SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal a " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "WHERE ac.status IN :statuses " +
      "AND (:animalType IS NULL OR a.animalType = :animalType) " +
      "AND (:location IS NULL OR ac.location LIKE %:location%) " +
      "AND ac.deletedAt IS NULL",
      countQuery = "SELECT COUNT(ac) FROM AnimalCase ac " +
          "LEFT JOIN ac.animal a " +
          "WHERE ac.status IN :statuses " +
          "AND (:animalType IS NULL OR a.animalType = :animalType) " +
          "AND (:location IS NULL OR ac.location LIKE %:location%) " +
          "AND ac.deletedAt IS NULL")
  Page<AnimalCase> findAllByStatusInAndFilters(
      @Param("statuses") Collection<CaseStatus> statuses,
      @Param("animalType") AnimalType animalType,
      @Param("location") String location,
      Pageable pageable
  );

  @Query(value = "SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "WHERE ac.currentFoster = :currentFoster AND ac.deletedAt IS NULL",
      countQuery = "SELECT COUNT(ac) FROM AnimalCase ac WHERE ac.currentFoster = :currentFoster AND ac.deletedAt IS NULL")
  Page<AnimalCase> findAllByCurrentFoster(
      @Param("currentFoster") Member currentFoster,
      Pageable pageable
  );

  @Query(value = "SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "WHERE ac.currentFoster = :currentFoster AND ac.status IN :statuses AND ac.deletedAt IS NULL",
      countQuery = "SELECT COUNT(ac) FROM AnimalCase ac WHERE ac.currentFoster = :currentFoster AND ac.status IN :statuses AND ac.deletedAt IS NULL")
  Page<AnimalCase> findAllByCurrentFosterAndStatusIn(
      @Param("currentFoster") Member currentFoster,
      @Param("statuses") Collection<CaseStatus> statuses,
      Pageable pageable
  );

  // 기본 findById 오버라이딩 - 소프트 삭제 필터링 추가
  @Query("SELECT ac FROM AnimalCase ac " +
      "LEFT JOIN FETCH ac.animal " +
      "LEFT JOIN FETCH ac.currentFoster " +
      "WHERE ac.id = :id AND ac.deletedAt IS NULL")
  Optional<AnimalCase> findById(@Param("id") Long id);

  Optional<AnimalCase> findByAnimalId(Long animalId);

  long countByCurrentFosterAndStatus(Member currentFoster, CaseStatus caseStatus);
}
