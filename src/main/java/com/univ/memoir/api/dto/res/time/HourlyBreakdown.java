package com.univ.memoir.api.dto.res.time;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class HourlyBreakdown {
    private int hour;
    private int totalUsageMinutes;
    private Map<String, Integer> categoryMinutes;
}