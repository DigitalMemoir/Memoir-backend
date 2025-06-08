package com.univ.memoir.api.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KeywordAnalysisResponse {
    private List<KeywordFrequencyDto> keywordFrequencies;
}
