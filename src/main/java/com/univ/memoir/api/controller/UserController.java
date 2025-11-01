package com.univ.memoir.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.univ.memoir.api.dto.req.user.UserInterestRequest;
import com.univ.memoir.api.dto.res.UserProfileDto;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회", description = "인증 컨텍스트의 이메일을 기반으로 사용자 프로필을 조회합니다.")
    public ResponseEntity<?> getUserProfileByToken(
            @AuthenticationPrincipal String email
    ) {
        User user = userService.findByEmail(email);
        return SuccessResponse.of(SuccessCode.USER_PROFILE_RETRIEVE_SUCCESS, new UserProfileDto(user));
    }

    @PostMapping("/category")
    @Operation(summary = "관심사 카테고리 선택", description = "사용자 관심사 카테고리를 선택합니다.")
    public ResponseEntity<?> updateInterestsByToken(
            @AuthenticationPrincipal String email,
            @RequestBody UserInterestRequest request
    ) {
        User updatedUser = userService.updateUserInterests(email, request.getInterests());
        return SuccessResponse.of(SuccessCode.UPDATED, new UserProfileDto(updatedUser));
    }
}