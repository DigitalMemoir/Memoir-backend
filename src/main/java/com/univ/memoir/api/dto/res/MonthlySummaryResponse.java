package com.univ.memoir.api.dto.res;

import java.util.List;

public class MonthlySummaryResponse {
	public record Data(
		int year,
		int month,
		List<CalendarEntry> calendarData
	) {}

	public record CalendarEntry(String date, String title) {}
}
