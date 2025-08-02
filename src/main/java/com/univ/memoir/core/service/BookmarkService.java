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
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * 북마크 목록 조회
     */
    public SuccessResponse<Set<String>> getBookmarks(String accessToken) {
        User user = findActiveUserByToken(accessToken);
        return SuccessResponse.of(SuccessCode.BOOKMARK_RETRIEVE_SUCCESS, user.getBookmarks()).getBody();
    }

    /**
     * 북마크 추가
     */
    @Transactional
    public SuccessResponse<String> addBookmark(String accessToken, BookmarkRequestDto requestDto) {
        User user = findActiveUserByToken(accessToken);
        user.addBookmarkUrl(requestDto.getUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_ADD_SUCCESS, requestDto.getUrl()).getBody();
    }

    /**
     * 북마크 삭제
     */
    @Transactional
    public SuccessResponse<String> removeBookmark(String accessToken, BookmarkRequestDto requestDto) {
        User user = findActiveUserByToken(accessToken);
        user.removeBookmarkUrl(requestDto.getUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_REMOVE_SUCCESS, requestDto.getUrl()).getBody();
    }

    /**
     * 북마크 수정 (기존 URL 삭제 후 새 URL 추가)
     */
    @Transactional
    public SuccessResponse<String> updateBookmark(String accessToken, BookmarkUpdateRequestDto requestDto) {
        User user = findActiveUserByToken(accessToken);
        user.removeBookmarkUrl(requestDto.getOldUrl());
        user.addBookmarkUrl(requestDto.getNewUrl());
        return SuccessResponse.of(SuccessCode.BOOKMARK_UPDATE_SUCCESS, requestDto.getNewUrl()).getBody();
    }

    /**
     * 토큰으로 활성 사용자 조회 (중복 제거)
     */
    private User findActiveUserByToken(String accessToken) {
        String email = authService.extractEmailFromToken(accessToken);

        return userRepository.findByEmail(email)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException(ErrorCode.USER_NOT_FOUND));
    }
}