package com.univ.memoir.api.controller;

import com.univ.memoir.api.dto.req.bookmark.BookmarkRequestDto;
import com.univ.memoir.api.dto.req.bookmark.BookmarkUpdateRequestDto;
import com.univ.memoir.api.exception.responses.SuccessResponse;
import com.univ.memoir.core.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bookmarks")
@Tag(name = "즐겨찾기", description = "즐겨찾기 관련 API")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @GetMapping
    @Operation(summary = "즐겨찾기 조회", description = "사용자의 즐겨찾기 목록을 조회합니다.")
    public ResponseEntity<SuccessResponse<Set<String>>> getBookmarks(
            @RequestHeader(name = "Authorization") String accessToken) {
        SuccessResponse<Set<String>> response = bookmarkService.getBookmarks(accessToken);
        return ResponseEntity.ok(response);
    }


    @PostMapping
    @Operation(summary = "즐겨찾기 추가", description = "즐겨찾기를 추가합니다.")
    public ResponseEntity<SuccessResponse<String>> addBookmark(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody BookmarkRequestDto requestDto) {
        SuccessResponse<String> response = bookmarkService.addBookmark(accessToken, requestDto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    @Operation(summary = "즐겨찾기 삭제", description = "즐겨찾기를 삭제합니다.")
    public ResponseEntity<SuccessResponse<String>> removeBookmark(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody BookmarkRequestDto requestDto) {
        SuccessResponse<String> response = bookmarkService.removeBookmark(accessToken, requestDto);
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    @Operation(summary = "즐겨찾기 수정", description = "즐겨찾기를 수정합니다.")
    public ResponseEntity<SuccessResponse<String>> updateBookmark(
            @RequestHeader("Authorization") String accessToken,
            @RequestBody BookmarkUpdateRequestDto requestDto) {
        SuccessResponse<String> response = bookmarkService.updateBookmark(accessToken, requestDto);
        return ResponseEntity.ok(response);
    }
}
