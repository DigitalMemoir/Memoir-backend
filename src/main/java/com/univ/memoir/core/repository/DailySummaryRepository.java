package com.univ.memoir.core.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.univ.memoir.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import com.univ.memoir.core.domain.DailySummary;

public interface DailySummaryRepository extends JpaRepository<DailySummary, Long> {
    Optional<DailySummary> findByUserAndDate(User user, LocalDate date);
	List<DailySummary> findAllByUserAndDateBetween(User user, LocalDate start, LocalDate end);
}
