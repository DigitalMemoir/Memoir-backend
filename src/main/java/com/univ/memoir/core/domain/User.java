package com.univ.memoir.core.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

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


}
