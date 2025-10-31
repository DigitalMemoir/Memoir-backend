package com.univ.memoir.api.controller;

import java.util.Set;

import com.univ.memoir.api.dto.req.bookmark.BookmarkUpdateRequestDto;
import com.univ.memoir.api.exception.codes.SuccessCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.univ.memoir.api.dto.req.bookmark.BookmarkRequestDto;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.BookmarkService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks")
@Tag(name = "즐겨찾기", description = "즐겨찾기 관련 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    @Operation(summary = "즐겨찾기 조회", description = "사용자의 즐겨찾기 목록을 조회합니다.")
    public ResponseEntity<SuccessResponse<Set<String>>> getBookmarks(
            @AuthenticationPrincipal String email) {
        Set<String> bookmarks = bookmarkService.getBookmarks(email);
        return SuccessResponse.of(SuccessCode.BOOKMARK_RETRIEVE_SUCCESS, bookmarks);
    }

    @PostMapping
    @Operation(summary = "즐겨찾기 추가", description = "즐겨찾기를 추가합니다.")
    public ResponseEntity<SuccessResponse<Void>> addBookmark(
            @AuthenticationPrincipal String email,
            @RequestBody BookmarkRequestDto requestDto) {

        bookmarkService.addBookmark(email, requestDto);
        return SuccessResponse.of(SuccessCode.BOOKMARK_ADD_SUCCESS);
    }

    @DeleteMapping
    @Operation(summary = "즐겨찾기 삭제", description = "즐겨찾기를 삭제합니다.")
    public ResponseEntity<SuccessResponse<Void>> removeBookmark(
            @AuthenticationPrincipal String email,
            @RequestBody BookmarkRequestDto requestDto) {
        bookmarkService.removeBookmark(email, requestDto);
        return SuccessResponse.of(SuccessCode.BOOKMARK_REMOVE_SUCCESS);
    }

    @PatchMapping
    @Operation(summary = "즐겨찾기 수정", description = "즐겨찾기를 수정합니다.")
    public ResponseEntity<SuccessResponse<Void>> updateBookmark(
            @AuthenticationPrincipal String email,
            @RequestBody BookmarkUpdateRequestDto requestDto) {
        bookmarkService.updateBookmark(email, requestDto);
        return SuccessResponse.of(SuccessCode.BOOKMARK_UPDATE_SUCCESS);
    }
}