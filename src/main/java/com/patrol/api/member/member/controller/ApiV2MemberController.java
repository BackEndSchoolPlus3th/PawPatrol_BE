package com.patrol.api.member.member.controller;

import com.patrol.api.animal.dto.MyPetListResponse;
import com.patrol.api.animal.dto.request.ModiPetInfoRequest;
import com.patrol.api.member.auth.dto.requestV2.ModifyProfileRequest;
import com.patrol.api.member.member.dto.request.PetRegisterRequest;
import com.patrol.domain.animal.service.AnimalService;
import com.patrol.domain.member.member.entity.Member;
import com.patrol.domain.member.member.service.V2MemberService;
import com.patrol.global.globalDto.GlobalResponse;
import com.patrol.global.webMvc.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * packageName    : com.patrol.api.member.member.controller
 * fileName       : ApiV2MemberController
 * author         : sungjun
 * date           : 2025-02-24
 * description    : 자동 주석 생성
 * ===========================================================
 * DATE              AUTHOR             NOTE
 * -----------------------------------------------------------
 * 2025-02-24        kyd54       최초 생성
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/members")
public class ApiV2MemberController {
    private final V2MemberService v2MemberService;
    private final AnimalService animalService;


    // 마이페이지 > 회원정보 수정
    @PatchMapping("/profile")
    public GlobalResponse<Void> modifyProfile(@LoginUser Member member,
                                              @RequestBody ModifyProfileRequest modifyProfileRequest) {

        v2MemberService.modifyProfile(member, modifyProfileRequest);

        return GlobalResponse.success();
    }

    // 마이페이지 > 반려동물 등록
    @PostMapping("/pets")
    public GlobalResponse<Void> petRegister(@LoginUser Member member,
                                            @ModelAttribute PetRegisterRequest petRegisterRequest) {

        animalService.myPetRegister(member, petRegisterRequest);

        return GlobalResponse.success();
    }

    // 마이페이지 > 내 반려동물 리스트
    @GetMapping("/pets")
    public GlobalResponse<List<MyPetListResponse>> myPetList(@LoginUser Member member) {

        List<MyPetListResponse> list = animalService.myPetList(member);

        return GlobalResponse.success(list);
    }
    
    // 마이페이지 > 내 반려동물 정보 수정
    @PatchMapping("/pets")
    public GlobalResponse<Void> modifyMyPetInfo(@LoginUser Member member,
                                                @ModelAttribute ModiPetInfoRequest modiPetInfoRequest) {

        System.out.println("==================="+ modiPetInfoRequest.getId());
        animalService.modifyMyPetInfo(member, modiPetInfoRequest);
        return GlobalResponse.success();
    }
}
