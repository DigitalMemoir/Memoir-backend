package com.univ.memoir.core.service;

import java.util.Set;

import com.univ.memoir.api.exception.customException.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.dto.req.bookmark.BookmarkRequestDto;
import com.univ.memoir.api.dto.req.bookmark.BookmarkUpdateRequestDto;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final UserRepository userRepository;

    public Set<String> getBookmarks(String email) {
        User user = userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        return user.getBookmarks();
    }

    @Transactional
    public void addBookmark(String email, BookmarkRequestDto requestDto) {
        User user = userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        user.addBookmarkUrl(requestDto.getUrl());
    }

    @Transactional
    public void removeBookmark(String email, BookmarkRequestDto requestDto) {
        User user = userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        user.removeBookmarkUrl(requestDto.getUrl());
    }

    @Transactional
    public void updateBookmark(String email, BookmarkUpdateRequestDto requestDto) {
        User user = userRepository.findByEmailWithDetails(email)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        user.removeBookmarkUrl(requestDto.getOldUrl());
        user.addBookmarkUrl(requestDto.getNewUrl());
    }
}