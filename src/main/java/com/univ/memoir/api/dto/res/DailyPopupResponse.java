package com.univ.memoir.api.dto.res;

import java.util.List;

public class DailyPopupResponse {
	public record Data(
		String date,
		List<String> title
	) {}
}
