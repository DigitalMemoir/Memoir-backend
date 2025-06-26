package com.univ.memoir.api.dto.res.time;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorySummary {
    private String category;
    private int totalTimeMinutes;
}