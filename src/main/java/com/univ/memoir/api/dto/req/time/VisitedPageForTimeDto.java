package com.univ.memoir.api.dto.req.time;

import lombok.Data;

@Data
public class VisitedPageForTimeDto {
    private String title;
    private String url;
    private int visitCount;
    private long startTimestamp;  // Unix timestamp in seconds
    private int durationSeconds;  // duration in seconds
}
