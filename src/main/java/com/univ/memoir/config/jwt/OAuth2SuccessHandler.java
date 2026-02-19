package com.univ.memoir.config.jwt;

import java.io.IOException;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.api.exception.custom.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Value("${oauth2.redirect-uri.githubpages}")
    private String githubPagesRedirectUri;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        User detachedUser = customOAuth2User.getUser();
        String email = detachedUser.getEmail();

        String accessToken = jwtProvider.createAccessToken(email);
        String refreshToken = jwtProvider.createRefreshToken(email);

        // detachedUser는 이미 이메일/ID를 보유하고 있으나 영속 컨텍스트가 없으므로
        // Dirty Checking을 위해 PK 기반으로 재조회 (email보다 PK가 인덱스 효율 우수)
        User managedUser = userRepository.findById(detachedUser.getId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        managedUser.updateAccessToken(accessToken);

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