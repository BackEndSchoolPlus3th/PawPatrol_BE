package com.patrol.domain.animal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.patrol.domain.animal.enums.AnimalGender;
import com.patrol.domain.animal.enums.AnimalSize;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.jpa.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;

/**
 * packageName    : com.patrol.domain.animal.entity
 * fileName       : Animal
 * author         : sungjun
 * date           : 2025-02-24
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-24        kyd54       최초 생성
 */
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "animals")
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public class Animal extends BaseEntity {
    private String breed;   // 품종

    @Enumerated(EnumType.STRING)
    private AnimalGender gender;    // 성별

    @Enumerated(EnumType.STRING)
    private AnimalSize size;    // 크기

    private String feature; // 특징
    private String healthCondition; // 건강상태
    private String name;    // 이름
    private String estimatedAge;    // 나이

    @ManyToOne(fetch = FetchType.LAZY)
    private Member owner;

    private String registrationNo;  // 동물등록번호

    private String imageUrl;  // 반려동물 사진

    @Enumerated(EnumType.STRING)
    private AnimalType animalType;  // 고양이, 강아지 구분

    private boolean isLost = false; // 실종여부

    @JsonIgnore
    @OneToMany(mappedBy = "animal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnimalCase> animalCases = new ArrayList<>();

    public void markAsLost() {
        this.isLost = true;
    }
}
