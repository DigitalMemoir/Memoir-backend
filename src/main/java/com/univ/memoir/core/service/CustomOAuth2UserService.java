package com.univ.memoir.core.service;

import java.util.HashMap;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.univ.memoir.config.jwt.CustomOAuth2User;
import com.univ.memoir.config.jwt.JwtProvider;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (googleId == null) {
            throw new OAuth2AuthenticationException("Google 계정 정보를 가져오지 못했습니다.");
        }

        String refreshToken = jwtProvider.createAccessToken(email);

        User user = userRepository.findByGoogleId(googleId).orElse(null);
        boolean isNewUser = false;

        if (user == null) {
            user = User.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .profileUrl(picture)
                    .refreshToken(refreshToken)
                    .build();
            userRepository.save(user);
            isNewUser = true;
        } else {
            boolean changed = false;

            if (!name.equals(user.getName())) {
                user.updateName(name);
                changed = true;
            }
            if (!picture.equals(user.getProfileUrl())) {
                user.updateProfileUrl(picture);
                changed = true;
            }
            if (!refreshToken.equals(user.getRefreshToken())) {
                user.updateRefreshToken(refreshToken);
                changed = true;
            }

            if (changed) {
                userRepository.save(user);
            }
        }

        // 기존 oAuth2User attributes 복사 후 isNewUser 속성 추가
        var attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("isNewUser", isNewUser);

        // CustomOAuth2User로 감싸서 반환
        return new CustomOAuth2User(user, attributes);
    }
}