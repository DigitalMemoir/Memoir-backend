package com.univ.memoir.api.dto.req.page;

import lombok.Data;

@Data
public class VisitedPageDto {
    private String title;
    private String url;
    private int visitCount;
    private long duration;
}
