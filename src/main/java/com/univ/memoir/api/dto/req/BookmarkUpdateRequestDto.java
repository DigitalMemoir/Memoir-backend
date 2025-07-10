package com.univ.memoir.api.dto.req;

import lombok.Getter;

@Getter
public class BookmarkUpdateRequestDto {
    private String oldUrl;
    private String newUrl;
}
