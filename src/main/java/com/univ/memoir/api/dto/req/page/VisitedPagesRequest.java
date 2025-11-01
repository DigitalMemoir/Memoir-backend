package com.univ.memoir.api.dto.req.page;

import java.util.List;

import com.univ.memoir.api.dto.req.page.VisitedPageDto;
import lombok.Data;

@Data
public class VisitedPagesRequest {
    private List<VisitedPageDto> visitedPages;
}
