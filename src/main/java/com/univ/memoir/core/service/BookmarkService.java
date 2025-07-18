package com.univ.memoir.core.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.univ.memoir.api.dto.req.bookmark.BookmarkRequestDto;
import com.univ.memoir.api.dto.req.bookmark.BookmarkUpdateRequestDto;
import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.codes.SuccessCode;
import com.univ.memoir.api.exception.customException.InvalidTokenException;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.config.jwt.JwtProvider;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public SuccessResponse<Set<String>> getBookmarks(String accessToken) {
        String email = jwtProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));

        return SuccessResponse.of(SuccessCode.BOOKMARK_RETRIEVE_SUCCESS, user.getBookmarks()).getBody();
    }


    @Transactional
    public SuccessResponse<String> addBookmark(String accessToken, BookmarkRequestDto requestDto) {
        String email  = jwtProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
        user.addBookmarkUrl(requestDto.getUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_ADD_SUCCESS, requestDto.getUrl()).getBody();
    }

    @Transactional
    public SuccessResponse<String> removeBookmark(String accessToken, BookmarkRequestDto requestDto) {
        String email  = jwtProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
        user.removeBookmarkUrl(requestDto.getUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_REMOVE_SUCCESS, requestDto.getUrl()).getBody();
    }

    @Transactional
    public SuccessResponse<String> updateBookmark(String accessToken, BookmarkUpdateRequestDto requestDto) {
        String email  = jwtProvider.getEmailFromToken(accessToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.INVALID_JWT_ACCESS_TOKEN));
        user.removeBookmarkUrl(requestDto.getOldUrl());
        user.addBookmarkUrl(requestDto.getNewUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_UPDATE_SUCCESS, requestDto.getNewUrl()).getBody();
    }
}
