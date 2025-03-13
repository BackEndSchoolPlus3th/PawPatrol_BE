package com.patrol.domain.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patrol.api.facility.dto.AdoptionAnimalApiResponse;
import com.patrol.api.facility.dto.AdoptionAnimalImageApiResponse;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.enums.AnimalGender;
import com.patrol.domain.animal.enums.AnimalSize;
import com.patrol.domain.animal.enums.AnimalType;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.animalCase.entity.AnimalCase;
import com.patrol.domain.animalCase.enums.CaseStatus;
import com.patrol.domain.animalCase.enums.ContentType;
import com.patrol.domain.animalCase.service.AnimalCaseService;
import com.patrol.domain.animalCase.service.CaseHistoryService;
import com.patrol.domain.facility.entity.OperatingHours;
import com.patrol.domain.facility.entity.Shelter;
import com.patrol.domain.facility.repository.ShelterRepository;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.repository.ImageRepository;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.enums.MemberRole;
import com.patrol.domain.member.member.enums.MemberStatus;
import com.patrol.domain.member.member.enums.ProviderType;
import com.patrol.domain.member.member.repository.MemberRepository;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;


@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdoptionAnimalService {

  private final AnimalRepository animalRepository;
  private final AnimalCaseService animalCaseService;
  private final ShelterRepository shelterRepository;
  private final ObjectMapper objectMapper;
  private final MemberRepository memberRepository;
  private final PasswordEncoder passwordEncoder;
  private final ImageRepository imageRepository;
  private final CaseHistoryService caseHistoryService;


  @Transactional
  public void initCenters() {
    OperatingHours operatingHours = OperatingHours.builder()
        .weekdayTime("09:00 - 18:00")
        .build();

    // 마포센터
    if (shelterRepository.findByName("서울동물복지지원센터 - 마포센터").isEmpty()) {
      Shelter mapoCenter = Shelter.builder()
          .name("서울동물복지지원센터 - 마포센터")
          .address("서울시 마포구 매봉산로 31 (지하1층)")
          .tel("02-2124-2839")
          .latitude(37.5757269662412)
          .longitude(126.889972593033)
          .operatingHours(operatingHours)
          .build();
      shelterRepository.save(mapoCenter);

      Member mapoMember = Member.builder()
          .email("center1@test.com")
          .status(MemberStatus.ACTIVE)
          .password(passwordEncoder.encode("1234"))
          .role(MemberRole.ROLE_SHELTER)
          .loginType(ProviderType.SELF)
          .apiKey(UUID.randomUUID().toString())
          .nickname(mapoCenter.getName())
          .marketingAgree(false)
          .build();
      memberRepository.save(mapoMember);
      mapoCenter.setShelterMember(mapoMember);
    }

    // 구로센터
    if (shelterRepository.findByName("서울동물복지지원센터 - 구로센터").isEmpty()) {
      Shelter guroCenter = Shelter.builder()
          .name("서울동물복지지원센터 - 구로센터")
          .address("서울 구로구 경인로 472")
          .tel("02-2636-7650")
          .latitude(37.498600545332)
          .longitude(126.871324534896)
          .operatingHours(operatingHours)
          .build();
      shelterRepository.save(guroCenter);

      Member guroMember = Member.builder()
          .email("center2@test.com")
          .status(MemberStatus.ACTIVE)
          .password(passwordEncoder.encode("1234"))
          .role(MemberRole.ROLE_SHELTER)
          .loginType(ProviderType.SELF)
          .apiKey(UUID.randomUUID().toString())
          .nickname(guroCenter.getName())
          .marketingAgree(false)
          .build();
      memberRepository.save(guroMember);
      guroCenter.setShelterMember(guroMember);
    }

    // 동대문센터
    if (shelterRepository.findByName("서울동물복지지원센터 - 동대문센터").isEmpty()) {
      Shelter dongdaemunCenter = Shelter.builder()
          .name("서울동물복지지원센터 - 동대문센터")
          .address("서울시 동대문구 무학로 201")
          .tel("02-921-2415")
          .latitude(37.5751234567890)  // 실제 좌표로 수정 필요
          .longitude(127.0301234567890)  // 실제 좌표로 수정 필요
          .operatingHours(operatingHours)
          .build();
      shelterRepository.save(dongdaemunCenter);

      Member dongdaemunMember = Member.builder()
          .email("center3@test.com")
          .status(MemberStatus.ACTIVE)
          .password(passwordEncoder.encode("1234"))
          .role(MemberRole.ROLE_SHELTER)
          .loginType(ProviderType.SELF)
          .apiKey(UUID.randomUUID().toString())
          .nickname(dongdaemunCenter.getName())
          .marketingAgree(false)
          .build();
      memberRepository.save(dongdaemunMember);
      dongdaemunCenter.setShelterMember(dongdaemunMember);
    }

    log.info("센터 정보 초기화 완료");
  }

  private String getCenterNameFromAnimalName(String name) {
    if (name == null || name.isEmpty()) {
      return "서울동물복지지원센터";
    }

    if (name.contains("마포센터")) {
      return "서울동물복지지원센터 - 마포센터";
    } else if (name.contains("구로센터")) {
      return "서울동물복지지원센터 - 구로센터";
    } else if (name.contains("동대문센터")) {
      return "서울동물복지지원센터 - 동대문센터";
    } else {
      return "서울동물복지지원센터"; // 기본 센터
    }
  }

  private Map<String, List<Image>> handleImages(String jsonImageResponse) {
    try {
      AdoptionAnimalImageApiResponse response =
          objectMapper.readValue(jsonImageResponse, AdoptionAnimalImageApiResponse.class);

      Map<String, List<Image>> animalImagesMap = new HashMap<>();
      if (response.getTbAdpWaitAnimalPhotoView() != null) {
        List<Image> imagesToSave = new ArrayList<>();

        for (AdoptionAnimalImageApiResponse.Row row : response.getTbAdpWaitAnimalPhotoView().getRow()) {
          String animalNo = row.getAnimalNo();
          String imageUrl = "https://" + row.getPhotoUrl();

          Image existingImage = imageRepository.findByPath(imageUrl);
          if (existingImage == null) {
            existingImage = Image.builder()
                .path("https://" + row.getPhotoUrl())
                .build();
            imagesToSave.add(existingImage);
          }

          if (!animalImagesMap.containsKey(animalNo)) {
            animalImagesMap.put(animalNo, new ArrayList<>());
          }
          animalImagesMap.get(animalNo).add(existingImage);
        }

        if (!imagesToSave.isEmpty()) {
          imageRepository.saveAll(imagesToSave);
          log.info("새로 저장된 입양대기 이미지 수: {}", imagesToSave.size());
        }

        log.info("처리된 입양대기 이미지 수: {}", response.getTbAdpWaitAnimalPhotoView().getRow().size());
      } else {
        log.warn("이미지 API에서 받은 데이터가 없거나 비어있습니다.");
      }

      return animalImagesMap;

    } catch (Exception e) {
      log.error("이미지 데이터 저장 중 에러 발생: {}", e.getMessage(), e);
      throw new RuntimeException("이미지 데이터 저장 실패", e);
    }
  }


  @Transactional
  public void saveApiResponse(String jsonResponse, String jsonImageResponse) {
    try {
      initCenters();

      Map<String, List<Image>> animalImagesMap = handleImages(jsonImageResponse);

      AdoptionAnimalApiResponse response = objectMapper.readValue(jsonResponse, AdoptionAnimalApiResponse.class);
      log.info("API 응답 파싱 결과: TbAdpWaitAnimalView={}",
          response.getTbAdpWaitAnimalView() != null ? "not null" : "null");

      if (response.getTbAdpWaitAnimalView() != null) {
        List<AnimalCase> animalCases = new ArrayList<>();

        for (AdoptionAnimalApiResponse.Row row : response.getTbAdpWaitAnimalView().getRow()) {
          log.info("동물 정보: ANIMAL_NO={}, NM={}", row.getAnimalNo(), row.getName());

          // 1. 기존 동물 확인 (동물번호로 검색)
          if (row.getAnimalNo() == null || row.getAnimalNo().isEmpty()) {
            log.warn("동물번호(ANIMAL_NO)가 null이거나 비어있습니다. 해당 데이터는 건너뜁니다.");
            continue;
          }

          Optional<Animal> existingAnimal = animalRepository.findByRegistrationNo(row.getAnimalNo());
          Animal animal;
          if (existingAnimal.isPresent()) {
            animal = updateAnimalFromRow(existingAnimal.get(), row);
          } else {
            animal = createAnimalFromRow(row);
            animalRepository.save(animal);
          }
          
          List<Image> animalImages = animalImagesMap.get(row.getAnimalNo());
          if (animalImages != null && !animalImages.isEmpty()) {
            for (Image image : animalImages) {
              image.setAnimalId(animal.getId());
            }
            Image representativeImage = animalImages.getFirst();
            animal.setImageUrl(representativeImage.getPath());
          }


          // 2. Shelter 지정하기
          String centerName = getCenterNameFromAnimalName(row.getName());
          Shelter shelter = shelterRepository.findByName(centerName)
              .orElseThrow(() -> new CustomException(ErrorCode.ENTITY_NOT_FOUND));

          // 3. AnimalCase 생성 또는 업데이트
          AnimalCase existingAnimalCase = animalCaseService.findByAnimal(animal);
          if (existingAnimalCase != null) {
            updateAnimalCaseFromRow(existingAnimalCase, row);
          } else {
            AnimalCase animalCase = createAnimalCaseFromRow(row, animal, shelter);
            animalCases.add(animalCase);

            caseHistoryService.addAnimalCase(
                animalCase, ContentType.ANIMAL_CASE, animalCase.getId(), shelter.getShelterMember().getId()
            );
          }
        }

        if (!animalCases.isEmpty()) {
          animalCaseService.saveAll(animalCases);
        }

        log.info("저장된 입양대기동물 수: {}", response.getTbAdpWaitAnimalView().getRow().size());
      } else {
        log.warn("API에서 받은 데이터가 없거나 비어있습니다.");
      }
    } catch (Exception e) {
      log.error("데이터 저장 중 에러 발생: {}", e.getMessage(), e);
      throw new RuntimeException("데이터 저장 실패", e);
    }
  }

  private Animal updateAnimalFromRow(Animal animal, AdoptionAnimalApiResponse.Row row) {
    animal.setName(extractName(row.getName()));
    animal.setBreed(row.getBreed());
    animal.setGender(row.getGender().equals("M") ? AnimalGender.M : AnimalGender.W);
    animal.setEstimatedAge(row.getAge());
    animal.setRegistrationNo(row.getAnimalNo());
    animal.setFeature("YouTube 영상 참고");
    animal.setHealthCondition("YouTube 영상 참고");
    animal.setAnimalType("DOG".equalsIgnoreCase(row.getSpecies()) ? AnimalType.DOG : AnimalType.CAT);
    animal.setSize(calculateSize(row.getWeight()));
    animal.setLost(false);
    return animal;
  }

  public String extractName(String input) {
    int parenthesisIndex = input.indexOf('(');
    if (parenthesisIndex == -1) {
      return input;
    }
    return input.substring(0, parenthesisIndex);
  }

  private Animal createAnimalFromRow(AdoptionAnimalApiResponse.Row row) {
    String healthCondition = "YouTube 영상 참고";
    String feature = "YouTube 영상 참고";

    return Animal.builder()
            .name(extractName(row.getName()))
            .breed(row.getBreed())
            .gender(row.getGender().equals("M") ? AnimalGender.M : AnimalGender.W)
            .estimatedAge(row.getAge())
            .healthCondition(healthCondition)
            .feature(feature)
            .registrationNo(row.getAnimalNo())
            .animalType("DOG".equalsIgnoreCase(row.getSpecies()) ? AnimalType.DOG : AnimalType.CAT)
            .size(calculateSize(row.getWeight()))
            .isLost(false)
            .build();
  }

  private AnimalSize calculateSize(String bdwgh) {
    try {
      if (bdwgh == null || bdwgh.isEmpty()) {
        return AnimalSize.MEDIUM;
      }

      double weight = Double.parseDouble(bdwgh);

      if (weight <= 3.0) {
        return AnimalSize.SMALL;
      } else if (weight <= 6.0) {
        return AnimalSize.MEDIUM;
      } else {
        return AnimalSize.LARGE;
      }
    } catch (Exception e) {
      log.warn("체중 변환 실패: {}", bdwgh);
      return AnimalSize.MEDIUM;
    }
  }

  private void updateAnimalCaseFromRow(AnimalCase animalCase, AdoptionAnimalApiResponse.Row row) {
    animalCase.setTitle(row.getName());
    animalCase.setDescription("영상 링크 : " + row.getVideoUrl());
  }

  private AnimalCase createAnimalCaseFromRow(AdoptionAnimalApiResponse.Row row, Animal animal, Shelter shelter) {
    AnimalCase animalCase = animalCaseService.createNewCase(CaseStatus.SHELTER_PROTECTING, animal);
    animalCase.setShelter(shelter);
    animalCase.setCurrentFoster(shelter.getShelterMember());
    animalCase.setTitle(row.getName());
    animalCase.setLocation(shelter.getName());
    animalCase.setDescription("영상 링크 : " + row.getVideoUrl());
    animalCase.getAnimal().setOwner(shelter.getShelterMember());
    return animalCase;
  }

}
