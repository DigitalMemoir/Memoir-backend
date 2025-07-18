package com.univ.memoir.core.repository;

import com.univ.memoir.core.domain.KeywordData;
import com.univ.memoir.core.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface KeywordDataRepository extends JpaRepository<KeywordData, Long> {
    List<KeywordData> findByUser(User user);

    List<KeywordData> findByUserAndCreatedAtBetween(User user, LocalDateTime startOfDay, LocalDateTime endOfDay);
}
