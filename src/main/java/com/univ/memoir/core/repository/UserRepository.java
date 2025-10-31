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

    @Query("SELECT DISTINCT u FROM User u " +
            "LEFT JOIN FETCH u.interests " +
            "LEFT JOIN FETCH u.bookmarkUrls " +
            "WHERE u.email = :email")
    Optional<User> findByEmailWithDetails(@Param("email") String email);
}