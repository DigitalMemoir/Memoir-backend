package com.univ.memoir.core.service;

import java.util.Set;

import com.univ.memoir.api.exception.GlobalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.custom.InvalidTokenException;
import com.univ.memoir.core.domain.InterestType;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));
    }

    public User findByEmailForSummary(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public User updateUserInterests(String email, Set<InterestType> interests) {
        User user = userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));

        user.updateInterests(interests);

        return user;
    }
}