package com.univ.memoir.api.dto.res.keyword;

import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponseDto {
    private List<KeywordFrequencyDto> keywordFrequencies;
}
