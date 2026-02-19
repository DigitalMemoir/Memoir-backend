package com.univ.memoir.core.domain;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "time_analysis_data", indexes = {
    @Index(name = "idx_time_analysis_user_date", columnList = "user_id, date")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimeAnalysisData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate date;
    private int totalUsageMinutes;

    @Column(columnDefinition = "TEXT")
    private String categorySummariesJson;

    @Column(columnDefinition = "TEXT")
    private String hourlyBreakdownsJson;

    public TimeAnalysisData(User user, LocalDate date, int totalUsageMinutes,
                           String categorySummariesJson, String hourlyBreakdownsJson) {
        this.user = user;
        this.date = date;
        this.totalUsageMinutes = totalUsageMinutes;
        this.categorySummariesJson = categorySummariesJson;
        this.hourlyBreakdownsJson = hourlyBreakdownsJson;
    }

    public void update(int totalUsageMinutes, String categorySummariesJson, String hourlyBreakdownsJson) {
        this.totalUsageMinutes = totalUsageMinutes;
        this.categorySummariesJson = categorySummariesJson;
        this.hourlyBreakdownsJson = hourlyBreakdownsJson;
    }
}