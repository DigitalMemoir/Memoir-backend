package com.univ.memoir.core.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", nullable = false, unique = true)
    private String googleId; // 구글 OAuth 고유 식별자 (sub)

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_url", length = 2048)
    private String profileUrl;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(length = 1, nullable = false)
    private String status = "N"; // 'N' = 정상 / 'Y' = 탈퇴

    @ElementCollection(targetClass = InterestType.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "interest")
    private Set<InterestType> interests = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "user_bookmarks",
            joinColumns = @JoinColumn(name = "user_id"),
            uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "bookmark_url"})
    )
    @Column(name = "bookmark_url", length = 512, nullable = false)
    private Set<String> bookmarkUrls = new HashSet<>();

    @Builder
    public User(String googleId, String email, String name, String profileUrl, String refreshToken) {
        this.googleId = googleId;
        this.email = email;
        this.name = name;
        this.profileUrl = profileUrl;
        this.refreshToken = refreshToken;
        this.status = "N";
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void withdraw() {
        this.status = "Y";
        this.refreshToken = null;
    }

    public boolean isActive() {
        return "N".equals(this.status);
    }

    public void updateInterests(Set<InterestType> newInterests) {
        this.interests.clear();
        this.interests.addAll(newInterests);
    }

    public void addBookmarkUrl(String url) {
        this.bookmarkUrls.add(url);
    }

    public void removeBookmarkUrl(String url) {
        this.bookmarkUrls.remove(url);
    }

    public Set<String> getBookmarks() {
        return bookmarkUrls;
    }
}
