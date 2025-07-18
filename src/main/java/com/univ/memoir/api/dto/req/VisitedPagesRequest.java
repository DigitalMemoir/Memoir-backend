package com.univ.memoir.api.dto.req;

import java.util.List;

import lombok.Data;

@Data
public class VisitedPagesRequest {
    private List<VisitedPageDto> visitedPages;
}
