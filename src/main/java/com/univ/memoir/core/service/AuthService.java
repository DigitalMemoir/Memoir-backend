package com.univ.memoir.core.service;

import com.univ.memoir.api.dto.res.AuthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.InvalidTokenException;
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

    /**
     * 리프레시 토큰으로 새로운 토큰 쌍 발급
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        // 1. 리프레시 토큰 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_REFRESH_TOKEN);
        }

        // 2. 사용자 조회
        String email = jwtProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));

        // 3. DB 저장된 리프레시 토큰과 비교
        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_REFRESH_TOKEN);
        }

        // 4. 새로운 토큰 쌍 생성
        String newAccessToken = jwtProvider.createAccessToken(email);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        // 5. 새 리프레시 토큰 저장
        user.updateRefreshToken(newRefreshToken);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(7 * 24 * 3600L) // 1주일 (초 단위)
                .isNewUser(false)
                .build();
    }

    /**
     * 로그아웃 - 리프레시 토큰 무효화
     */
    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.updateRefreshToken(null);
                    userRepository.save(user);
                });
    }

    /**
     * 토큰에서 이메일 추출 (UserService용)
     */
    public String extractEmailFromToken(String accessToken) {
        String cleanToken = accessToken.startsWith("Bearer ") ? 
                accessToken.substring(7).trim() : accessToken.trim();
        
        if (!jwtProvider.validateToken(cleanToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN);
        }
        
        return jwtProvider.getEmailFromToken(cleanToken);
    }
}