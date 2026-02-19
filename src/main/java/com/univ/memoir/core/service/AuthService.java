package com.univ.memoir.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.dto.res.AuthResponse;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.custom.InvalidTokenException;
import com.univ.memoir.config.jwt.JwtProvider;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final UserService userService;

    public AuthResponse refreshAccessToken(String refreshToken) {
        // 1단계: 토큰 검증 + 이메일 추출 (비DB 작업 — 트랜잭션 불필요)
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_REFRESH_TOKEN);
        }
        String email = jwtProvider.getEmailFromToken(refreshToken);

        // 2단계: JWT 생성 (순수 연산 — 트랜잭션 불필요)
        // 기존: @Transactional 내에서 JWT 생성 → DB 커넥션을 JWT 생성 시간 동안 불필요하게 점유
        // 변경: JWT 생성을 먼저 수행하고, DB 작업(조회+저장)만 트랜잭션으로 묶음
        String newAccessToken = jwtProvider.createAccessToken(email);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        // 3단계: DB 조회 + 저장 — self-invocation은 AOP 프록시를 우회하므로
        // @Transactional이 보장된 UserService에 위임
        userService.updateAccessToken(email, newAccessToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(7 * 24 * 3600L)
                .isNewUser(false)
                .build();
    }

    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.updateAccessToken(null);
                });
    }

    public String extractEmailFromToken(String accessToken) {
        String cleanToken = accessToken.startsWith("Bearer ") ?
                accessToken.substring(7).trim() : accessToken.trim();

        if (!jwtProvider.validateToken(cleanToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN);
        }

        return jwtProvider.getEmailFromToken(cleanToken);
    }
}