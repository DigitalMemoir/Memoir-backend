package com.univ.memoir.config.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Value("${oauth2.redirect-uri}")
    private String defaultRedirectUri;

    @Value("${oauth2.redirect-uri.onboarding}")
    private String onboardingRedirectUri;

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String accessToken = jwtProvider.createAccessToken(email);

        // 신규 회원 여부 판별
        Boolean isNewUserAttr = oAuth2User.getAttribute("isNewUser");
        boolean isNewUser = Boolean.TRUE.equals(isNewUserAttr);

        // 리디렉트 URI 선택
        String baseRedirectUri = isNewUser ? onboardingRedirectUri : defaultRedirectUri;

        // UriComponentsBuilder로 accessToken 포함해서 URL 생성
        String targetUrl = UriComponentsBuilder
                .fromUriString(baseRedirectUri)
                .queryParam("token", accessToken)
                .queryParam("isNewUser", isNewUser)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}
