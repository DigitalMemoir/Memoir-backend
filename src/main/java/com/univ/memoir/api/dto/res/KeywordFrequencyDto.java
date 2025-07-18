package com.univ.memoir.api.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordFrequencyDto {
    private String keyword;
    private int frequency;
}
