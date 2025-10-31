package com.univ.memoir.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.univ.memoir.core.domain.KeywordData;
import com.univ.memoir.core.domain.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KeywordDataRepository extends JpaRepository<KeywordData, Long> {
    @Query("SELECT kd FROM KeywordData kd WHERE kd.user.id = :userId AND kd.createdAt BETWEEN :startOfDay AND :endOfDay")
    List<KeywordData> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}
