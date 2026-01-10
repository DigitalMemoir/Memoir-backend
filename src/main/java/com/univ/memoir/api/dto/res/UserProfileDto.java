package com.univ.memoir.api.dto.res;

import com.univ.memoir.core.domain.User;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDto {
    private Long id;
    private String email;
    private String name;
    private String profileUrl;

    public UserProfileDto(User user){
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.profileUrl = user.getProfileUrl();
    }
}