package com.univ.memoir.config.jwt;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback"; // 프론트 콜백 주소

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // AccessToken 생성
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String accessToken = jwtProvider.createToken(email);


        // AccessToken을 HttpOnly 쿠키로 설정
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true); // JavaScript에서 접근 불가
        accessTokenCookie.setSecure(false);  // 개발 단계에서는 false
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(30 * 60); // 30분

        response.addCookie(accessTokenCookie);

        // 프론트로 리디렉트
        response.sendRedirect(REDIRECT_URI);
    }
}
