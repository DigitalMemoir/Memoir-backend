package com.univ.memoir.core.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.univ.memoir.api.dto.res.KeywordFrequencyDto;
import com.univ.memoir.core.domain.KeywordData;

public interface KeywordDataRepository extends JpaRepository<KeywordData, Long> {

    @Query("SELECT kd FROM KeywordData kd WHERE kd.user.id = :userId AND kd.createdAt BETWEEN :startOfDay AND :endOfDay")
    List<KeywordData> findByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * DB 레벨에서 GROUP BY + SUM + ORDER BY + LIMIT 처리.
     * 기존 Java 스트림 groupingBy/sorted/limit 대체.
     */
    @Query("SELECT new com.univ.memoir.api.dto.res.KeywordFrequencyDto(kd.keyword, CAST(SUM(kd.frequency) AS int)) " +
           "FROM KeywordData kd " +
           "WHERE kd.user.id = :userId AND kd.createdAt BETWEEN :startOfDay AND :endOfDay " +
           "GROUP BY kd.keyword " +
           "ORDER BY SUM(kd.frequency) DESC")
    List<KeywordFrequencyDto> findTopKeywordsByUserIdAndDate(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    /**
     * SELECT 없이 조건에 맞는 행을 한 번에 삭제 (벌크 DELETE).
     * 기존 findAll → deleteAll 패턴 대체.
     */
    @Modifying
    @Query("DELETE FROM KeywordData kd WHERE kd.user.id = :userId AND kd.createdAt BETWEEN :startOfDay AND :endOfDay")
    void deleteByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    /**
     * 전체 행을 로드하지 않고 count만 조회.
     * asyncCacheRefreshCheck에서 사용.
     */
    @Query("SELECT COUNT(kd) FROM KeywordData kd WHERE kd.user.id = :userId AND kd.createdAt BETWEEN :startOfDay AND :endOfDay")
    long countByUserIdAndCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );
}