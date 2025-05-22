package com.univ.memoir.core.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email")
    private String email;

    @Column(name = "name")
    private String name;

    @Column(name = "profile_url", length = 2048)
    private String profileUrl;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Builder
    public User(String email, String name, String profileUrl, String refreshToken) {
        this.email = email;
        this.name = name;
        this.profileUrl = profileUrl;
        this.refreshToken = refreshToken;
    }

}

