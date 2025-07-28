package com.univ.memoir.config.jwt;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Value("${oauth2.redirect-uri.githubpages}")
    private String githubPagesRedirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String email = customOAuth2User.getUser().getEmail();

        String accessToken = jwtProvider.createAccessToken(email);
        String refreshToken = jwtProvider.createRefreshToken(email);

        Boolean isNewUserAttr = customOAuth2User.getAttribute("isNewUser");
        boolean isNewUser = Boolean.TRUE.equals(isNewUserAttr);

        String targetUrl = UriComponentsBuilder
                .fromUriString(githubPagesRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("expiresIn", 7 * 24 * 3600)
                .queryParam("isNewUser", isNewUser)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }
}