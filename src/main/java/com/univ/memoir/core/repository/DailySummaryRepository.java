package com.univ.memoir.core.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univ.memoir.core.domain.DailySummary;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {
	Optional<DailySummary> findByDate(LocalDate date);
	List<DailySummary> findAllByDateBetween(LocalDate start, LocalDate end);
}
