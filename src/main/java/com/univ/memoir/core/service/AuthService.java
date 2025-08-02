package com.univ.memoir.core.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.dto.res.AuthResponse;
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
     * 리프레시 토큰으로 새로운 토큰 쌍 발급 (간단 버전)
     */
    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_REFRESH_TOKEN);
        }

        String email = jwtProvider.getEmailFromToken(refreshToken);

        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(email);
        String newRefreshToken = jwtProvider.createRefreshToken(email);

        user.updateAccessToken(newAccessToken);
        userRepository.save(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(7 * 24 * 3600L)
                .isNewUser(false)
                .build();
    }

    /**
     * 로그아웃 (간단 버전)
     */
    @Transactional
    public void logout(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.updateAccessToken(null);
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