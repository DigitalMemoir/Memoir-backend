package com.univ.memoir.api.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KeywordFrequencyDto {
    private String keyword;
    private int frequency;
}
