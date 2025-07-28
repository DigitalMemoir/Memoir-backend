// ===== 2단계: AuthResponse.java 생성 =====
// 경로: src/main/java/com/univ/memoir/core/dto/AuthResponse.java

package com.univ.memoir.api.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // 초 단위
    private boolean isNewUser;
    
    // 편의 메서드들
    public static AuthResponse of(String accessToken, String refreshToken, boolean isNewUser) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(7 * 24 * 3600L) // 1주일
                .isNewUser(isNewUser)
                .build();
    }
}