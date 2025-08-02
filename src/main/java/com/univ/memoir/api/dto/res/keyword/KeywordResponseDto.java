package com.univ.memoir.api.dto.res.keyword;

import java.util.List;

import com.univ.memoir.api.dto.res.KeywordFrequencyDto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordResponseDto {
    private List<KeywordFrequencyDto> keywordFrequencies;
}
