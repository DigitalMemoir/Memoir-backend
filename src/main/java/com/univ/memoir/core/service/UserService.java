package com.univ.memoir.core.service;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.customException.InvalidTokenException;
import com.univ.memoir.api.exception.customException.UserNotFoundException;
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

    public Long getUserIdFromToken(String accessToken) {
        return userRepository.findByRefreshToken(accessToken)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    @Transactional
    public User updateUserInterestsByToken(String accessToken, Set<InterestType> interests) {
        User user = userRepository.findByRefreshToken(accessToken)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
        user.updateInterests(interests);
        return user;
    }

    public List<String> getUserInterests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        return user.getInterests()
                .stream()
                .map(InterestType::getCategoryName) // 예: "STUDY", "NEWS" 등의 문자열 반환
                .collect(Collectors.toList());
    }



}
