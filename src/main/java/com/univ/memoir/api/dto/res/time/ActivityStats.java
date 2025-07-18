package com.univ.memoir.api.dto.res.time;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ActivityStats {
    private int totalUsageTimeMinutes;
    private List<CategorySummary> categorySummaries;
    private List<HourlyBreakdown> hourlyActivityBreakdown;
}
