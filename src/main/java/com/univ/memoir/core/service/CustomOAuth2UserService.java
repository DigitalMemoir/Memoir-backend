package com.univ.memoir.core.service;

import com.univ.memoir.api.exception.codes.ErrorCode;
import com.univ.memoir.config.jwt.CustomOAuth2User;
import com.univ.memoir.config.jwt.JwtProvider;
import com.univ.memoir.core.domain.User;
import com.univ.memoir.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

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
            throw new OAuth2AuthenticationException("Google 계정 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }

        String refreshToken = jwtProvider.createRefreshToken(email);

        User user = userRepository.findByGoogleId(googleId)
                .map(existingUser -> {
                    // 기존 사용자 정보 업데이트 가능
                    existingUser.updateName(name);
                    existingUser.updateProfileUrl(picture);
                    existingUser.updateRefreshToken(refreshToken);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .googleId(googleId)
                        .email(email)
                        .name(name)
                        .profileUrl(picture)
                        .refreshToken(refreshToken)
                        .build()));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}