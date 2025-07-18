package com.univ.memoir.core.service;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.InvalidTokenException;
import com.univ.memoir.api.exception.customException.UserNotFoundException;
import com.univ.memoir.config.jwt.JwtProvider;
import com.univ.memoir.core.domain.InterestType;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public User updateUserInterestsByToken(String accessToken, Set<InterestType> interests) {
        String email = jwtProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
        user.updateInterests(interests);
        return user;
    }

    public User findByAccessToken(String accessToken) {
        // Bearer 접두사 제거
        if (accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7).trim();
        }

        // 토큰 유효성 검사
        if (!jwtProvider.validateToken(accessToken)) {
            throw new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN);
        }

        // 이메일 추출
        String email = jwtProvider.getEmailFromToken(accessToken);

        // 사용자 조회
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
    }

}
