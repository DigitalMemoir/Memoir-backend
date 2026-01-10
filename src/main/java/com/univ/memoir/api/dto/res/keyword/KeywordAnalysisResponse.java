package com.univ.memoir.api.dto.res.keyword;

import java.util.List;

import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeywordAnalysisResponse {
    private List<KeywordFrequencyDto> keywordFrequencies;
}
