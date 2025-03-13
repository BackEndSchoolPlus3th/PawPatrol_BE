package com.patrol.global.initData;


import com.patrol.api.member.auth.dto.request.SignupRequest;
import com.patrol.api.member.member.dto.request.PetRegisterRequest;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.animal.service.AnimalService;
import com.patrol.domain.animalCase.service.AnimalCaseEventPublisher;
import com.patrol.domain.image.service.ImageHandlerService;
import com.patrol.domain.member.auth.service.AuthService;
import com.patrol.domain.member.auth.service.V2AuthService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.domain.member.member.repository.V2MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!prod")
public class NotProd {
  @Bean
  public ApplicationRunner applicationRunner(
      V2AuthService authService,
      AnimalService animalService,
      AnimalCaseEventPublisher animalCaseEventPublisher,
      AnimalRepository animalRepository,
      V2MemberRepository memberRepository,
      ImageHandlerService imageHandlerService

  ) {
    return new ApplicationRunner() {
      @Transactional
      @Override
      public void run(ApplicationArguments args) throws Exception {
        // Member1,2,3 생성
        SignupRequest request1 = new SignupRequest("test1@test.com", "1234", "관리자", "서울");
        SignupRequest request2 = new SignupRequest("test2@test.com", "1234", "홍길동", "경기도");
        SignupRequest request3 = new SignupRequest("test3@test.com", "1234", "제펫토", "강원도");
        SignupRequest request4 = new SignupRequest("test4@test.com", "1234", "사자", "충청도");
        SignupRequest request5 = new SignupRequest("test5@test.com", "1234", "토끼", "인천");
        SignupRequest request6 = new SignupRequest("test6@test.com", "1234", "콩나물", "서울");

        Member member1 = authService.signUp(request1);
        Member member2 = authService.signUp(request2);
        Member member3 = authService.signUp(request3);
        Member member4 = authService.signUp(request4);
        Member member5 = authService.signUp(request5);
        Member member6 = authService.signUp(request6);

        long member1Id = member1.getId();

        // ID로 멤버 조회
        Member mem1 = memberRepository.findById(member1Id).orElseThrow(() ->
                new EntityNotFoundException("Member not found with id: " + member1Id));

        // 멤버3 권한 변경 (ADMIN)
        mem1.setRole(MemberRole.ROLE_ADMIN);
        mem1.setLoginType(ProviderType.SELF);
        mem1.setStatus(MemberStatus.ACTIVE);
        mem1.setProfileImageUrl("default.png");
        memberRepository.save(mem1);


        List<PetRegisterRequest> sampleAnimals = SampleAnimalData.getSampleStrayAnimals();
        List<String> imageUrls = SampleAnimalData.getSampleImageUrls();
        List<String> animalNameList = SampleAnimalData.getAnimalNameList();

        // 생성된 동물들을 저장할 리스트
        List<Animal> animalList = new ArrayList<>();

        // 동물 생성 (40마리)
        int animalCount = Math.min(40, sampleAnimals.size());
        for (int i = 0; i < animalCount; i++) {
          PetRegisterRequest animalRequest = sampleAnimals.get(i);
          String imageUrl = imageUrls.get(i);
          String name = animalNameList.get(i);

          Animal animal = animalService.registerWithImageUrl(animalRequest, imageUrl);
          animal.setName(name);
          animal.setLost(false);
          animalList.add(animal);

          System.out.println("샘플 데이터 생성: " + animal.getName() +
              " - " + animal.getBreed() + " (" + animal.getImageUrl() + ")");
        }

        // 임시보호용 게시글 타이틀 가져오기
        List<String> protectionTitles = SampleAnimalData.getSampleTitles();

        // member1 - 20마리
        int casesForMember1 = Math.min(20, animalList.size());
        for (int i = 0; i < casesForMember1; i++) {
          animalCaseEventPublisher.createAnimalCase(
              member1, animalList.get(i), protectionTitles.get(i), null, "서울 마포구"
          );
          System.out.println("Member1의 동물 케이스 생성: " + animalList.get(i).getBreed());
        }

        // member2 - 10마리
        int casesForMember2 = Math.min(10, animalList.size() - casesForMember1);
        for (int i = 0; i < casesForMember2; i++) {
          int index = casesForMember1 + i;
          if (index < animalList.size()) {
            animalCaseEventPublisher.createAnimalCase(
                member2, animalList.get(index), protectionTitles.get(index), null, "서울 송파구"
            );
            System.out.println("Member2의 동물 케이스 생성: " + animalList.get(index).getBreed());
          }
        }

        // member3 - 10마리
        int startIndexForMember3 = casesForMember1 + casesForMember2;
        int casesForMember3 = Math.min(10, animalList.size() - startIndexForMember3);
        for (int i = 0; i < casesForMember3; i++) {
          int index = startIndexForMember3 + i;
          if (index < animalList.size()) {
            animalCaseEventPublisher.createAnimalCase(
                member3, animalList.get(index), protectionTitles.get(index), null, "경기도 안양시"
            );
            System.out.println("Member3의 동물 케이스 생성: " + animalList.get(index).getBreed());
          }
        }

// ==================== 내 반려동물 데이터 추가 ====================
// 반려동물 샘플 데이터 가져오기
        List<PetRegisterRequest> myPets = SampleMyPetData.getSamplePets();
        List<String> myPetImageUrls = SampleMyPetData.getSampleImageUrls();

// 각 회원별로 2마리씩 반려동물 등록
        for (int i = 0; i < myPets.size(); i++) {
          // 회원 할당 (0,1,2번 동물은 member2, 3,4,5번 동물은 member3)
          Member owner = (i < 3) ? member2 : (i < 6) ? member3 : member1;

          // Animal 객체 생성
          Animal pet = Animal.builder()
                  .owner(owner)
                  .name(myPets.get(i).name())
                  .registrationNo(myPets.get(i).registrationNo())
                  .animalType(myPets.get(i).animalType())
                  .breed(myPets.get(i).breed())
                  .gender(myPets.get(i).gender())
                  .size(myPets.get(i).size())
                  .imageUrl(myPetImageUrls.get(i))
                  .estimatedAge(myPets.get(i).estimatedAge())
                  .healthCondition(myPets.get(i).healthCondition())
                  .feature(myPets.get(i).feature())
                  .build();

          animalRepository.save(pet);

          imageHandlerService.registerImage(myPetImageUrls.get(i), pet.getId(), null, null, pet.getAnimalType());
          // Animal 객체 저장
        }
      }
    };
  }
}
