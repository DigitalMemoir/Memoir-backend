package com.univ.memoir.api.dto.res.keyword;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeywordAnalysisResponse {
    private List<KeywordFrequencyDto> keywordFrequencies;
}
