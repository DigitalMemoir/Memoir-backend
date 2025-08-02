package com.univ.memoir.core.domain;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "daily_summary")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySummary {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	private LocalDate date;

	@Column(columnDefinition = "TEXT")
	private String topKeywordsJson;

	@Column(columnDefinition = "TEXT")
	private String timelineJson;

	@Column(columnDefinition = "TEXT")
	private String summaryTextJson;

	private int totalUsageMinutes;

	@Column(columnDefinition = "TEXT")
	private String activityProportionsJson;

	public DailySummary(User user, LocalDate date,
						String topKeywordsJson,
						String timelineJson,
						String summaryTextJson,
						int totalUsageMinutes,
						String activityProportionsJson) {
		this.user = user;
		this.date = date;
		this.topKeywordsJson = topKeywordsJson;
		this.timelineJson = timelineJson;
		this.summaryTextJson = summaryTextJson;
		this.totalUsageMinutes = totalUsageMinutes;
		this.activityProportionsJson = activityProportionsJson;
	}
}