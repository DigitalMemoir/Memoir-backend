package com.univ.memoir.api.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class VisitedPagesRequest {
    private List<VisitedPageDto> visitedPages;
}
