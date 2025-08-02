package com.univ.memoir.core.repository;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.univ.memoir.core.domain.TimeAnalysisData;
import com.univ.memoir.core.domain.User;

public interface TimeAnalysisDataRepository extends JpaRepository<TimeAnalysisData, Long> {
    Optional<TimeAnalysisData> findByUserAndDate(User user, LocalDate date);
}