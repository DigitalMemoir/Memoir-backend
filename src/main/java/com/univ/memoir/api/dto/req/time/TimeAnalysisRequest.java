package com.univ.memoir.api.dto.req.time;

import java.util.List;

import lombok.Data;

@Data
public class TimeAnalysisRequest {
    private String date;
    private List<VisitedPageForTimeDto> visitedPages;
}
