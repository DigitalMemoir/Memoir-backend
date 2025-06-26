package com.univ.memoir.api.dto.req.time;

import lombok.Data;

import java.util.List;

@Data
public class TimeAnalysisRequest {
    private String date;
    private List<VisitedPageForTimeDto> visitedPages;
}
