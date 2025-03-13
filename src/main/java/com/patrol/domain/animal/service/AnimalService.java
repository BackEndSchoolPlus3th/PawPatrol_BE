package com.patrol.domain.animal.service;

import com.patrol.api.animal.dto.MyPetListResponse;
import com.patrol.api.animal.dto.PetResponseDto;
import com.patrol.api.animal.dto.request.ModiPetInfoRequest;
import com.patrol.api.member.member.dto.request.PetRegisterRequest;
import com.patrol.domain.animal.entity.Animal;
import com.patrol.domain.animal.repository.AnimalRepository;
import com.patrol.domain.animalCase.service.AnimalCaseEventPublisher;
import com.patrol.domain.image.entity.Image;
import com.patrol.domain.image.service.ImageHandlerService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.global.error.ErrorCode;
import com.patrol.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnimalService {
    private final AnimalRepository animalRepository;
    private final ImageHandlerService imageHandlerService;
    private final AnimalCaseEventPublisher animalCaseEventPublisher;
    private static final String HOMELESS_FOLDER_PATH = "petRegister/homeless/";
    private static final String MEMBER_FOLDER_PATH_PREFIX = "petRegister/";

    @Transactional
    public void petRegister(PetRegisterRequest petRegisterRequest) {

        List<Image> savedImages = imageHandlerService.uploadAndRegisterImages(
                List.of(petRegisterRequest.imageFile()),
                HOMELESS_FOLDER_PATH,
                null,
                null,
                null,
                petRegisterRequest.animalType()
        );

        if (!savedImages.isEmpty()) {
            String imageUrl = savedImages.get(0).getPath();
            Animal animal = petRegisterRequest.buildAnimal(imageUrl);
            Animal savedAnimal = animalRepository.save(animal);
            Image image = savedImages.get(0);
            image.setAnimalId(savedAnimal.getId());
        } else {
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    @Transactional
    public void myPetRegister(Member member, PetRegisterRequest petRegisterRequest) {
        String folderPath = MEMBER_FOLDER_PATH_PREFIX + member.getId() + "/";
        List<Image> savedImages = imageHandlerService.uploadAndRegisterImages(
                List.of(petRegisterRequest.imageFile()),
                folderPath,
                null,
                null,
                null,
                petRegisterRequest.animalType()
        );

        if (!savedImages.isEmpty()) {
            String imageUrl = savedImages.get(0).getPath();
            Animal animal = petRegisterRequest.buildAnimal(member, imageUrl);
            Animal savedAnimal = animalRepository.save(animal);
            Image image = savedImages.get(0);
            image.setAnimalId(savedAnimal.getId());

            animalCaseEventPublisher.createMyPet(member, animal);
        } else {
            throw new CustomException(ErrorCode.FILE_UPLOAD_ERROR);
        }
    }

    public Optional<Animal> findById(Long animalId) {
        return animalRepository.findById(animalId);
    }

    @Transactional
    public Page<MyPetListResponse> myPetList(Member member, Pageable pageable) {
        Page<Animal> animalPage = animalRepository.findByOwnerId(member.getId(), pageable);

        return animalPage.map(animal -> MyPetListResponse.builder()
                .id(animal.getId())
                .name(animal.getName())
                .breed(animal.getBreed())
                .feature(animal.getFeature())
                .estimatedAge(animal.getEstimatedAge())
                .healthCondition(animal.getHealthCondition())
                .size(animal.getSize())
                .registrationNo(animal.getRegistrationNo())
                .imageUrl(animal.getImageUrl())
                .gender(animal.getGender())
                .animalType(animal.getAnimalType())
                .build());
    }

    @Transactional
    public void modifyMyPetInfo(Member member, ModiPetInfoRequest modiPetInfoRequest) {
        Animal animal = animalRepository.findById(modiPetInfoRequest.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ANIMAL_NOT_FOUND));

        validateOwner(animal, member);

        Optional.ofNullable(modiPetInfoRequest.getEstimatedAge()).ifPresent(animal::setEstimatedAge);
        Optional.ofNullable(modiPetInfoRequest.getFeature()).ifPresent(animal::setFeature);
        Optional.ofNullable(modiPetInfoRequest.getHealthCondition()).ifPresent(animal::setHealthCondition);
        Optional.ofNullable(modiPetInfoRequest.getSize()).ifPresent(animal::setSize);
        Optional.ofNullable(modiPetInfoRequest.getRegistrationNo()).ifPresent(animal::setRegistrationNo);

        if (modiPetInfoRequest.getImageFile() != null && !modiPetInfoRequest.getImageFile().isEmpty()) {
            String folderPath = MEMBER_FOLDER_PATH_PREFIX + member.getId() + "/";

            List<Image> savedImages = imageHandlerService.uploadAndModifiedImages(
                    List.of(modiPetInfoRequest.getImageFile()),
                    folderPath,
                    animal.getId()
            );

            if (!savedImages.isEmpty()) {
                if (animal.getImageUrl() != null && !animal.getImageUrl().isEmpty()) {
                    imageHandlerService.deleteImageByPath(animal.getImageUrl());
                }
                animal.setImageUrl(savedImages.get(0).getPath());
            }
        }
    }

    @Transactional
    public void deleteMyPetInfo(Member member, Long petId) {
        Animal animal = animalRepository.findById(petId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANIMAL_NOT_FOUND));

        validateOwner(animal, member);

        if (animal.getImageUrl() != null && !animal.getImageUrl().isEmpty()) {

            String objectKey = animal.getImageUrl().replace("https://kr.object.ncloudstorage.com/paw-patrol/", "");

            imageHandlerService.deleteImageByPath(objectKey);
        }

        animalRepository.delete(animal);
    }

    public void validateOwner(Animal animal, Member member) {
        if (!Objects.equals(animal.getOwner().getId(), member.getId())) {
            throw new CustomException(ErrorCode.PET_OWNER_MISMATCH);
        }
    }

    public List<PetResponseDto> getAllAnimals() {
        return animalRepository.findAll().stream()
                .map(PetResponseDto::new)
                .collect(Collectors.toList());
    }
}
