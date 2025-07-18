package com.univ.memoir.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univ.memoir.core.domain.KeywordData;
import com.univ.memoir.core.domain.User;

public interface KeywordDataRepository extends JpaRepository<KeywordData, Long> {
    List<KeywordData> findByUser(User user);

    List<KeywordData> findByUserAndCreatedAtBetween(User user, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
