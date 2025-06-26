package com.univ.memoir.api.dto.res.time;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ActivityStats {
    private int totalUsageTimeMinutes;
    private List<CategorySummary> categorySummaries;
    private List<HourlyBreakdown> hourlyActivityBreakdown;
}
