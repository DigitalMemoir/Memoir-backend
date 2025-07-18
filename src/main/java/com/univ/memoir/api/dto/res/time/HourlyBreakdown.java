package com.univ.memoir.api.dto.res.time;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HourlyBreakdown {
    private int hour;
    private int totalUsageMinutes;
    private Map<String, Integer> categoryMinutes;
}