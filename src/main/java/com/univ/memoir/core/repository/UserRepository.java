package com.univ.memoir.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.univ.memoir.core.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmail(String email);

    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findIdByEmail(@Param("email") String email);

    /**
     * interests + bookmarkUrls 모두 필요할 때 사용.
     * 두 컬렉션을 동시에 JOIN FETCH하면 카르테시안 곱이 발생하므로
     * DISTINCT로 중복 제거. (프로필 전체 조회 등)
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.interests " +
            "LEFT JOIN FETCH u.bookmarkUrls " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithDetails(@Param("email") String email);

    /**
     * interests만 필요할 때 사용. (관심사 업데이트 등)
     * bookmarkUrls를 함께 로드하지 않아 카르테시안 곱 없음.
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.interests " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithInterests(@Param("email") String email);

    /**
     * bookmarkUrls만 필요할 때 사용. (북마크 CRUD 등)
     * interests를 함께 로드하지 않아 카르테시안 곱 없음.
     */
    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.bookmarkUrls " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithBookmarks(@Param("email") String email);
}