package kr.cocoh.api.security.oauth2;

import java.time.LocalDateTime;
import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // OAuth2 서비스 ID (google, kakao, naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        // OAuth2 로그인 진행 시 키가 되는 필드 값 (PK)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserService를 통해 가져온 데이터를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(
                registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes, registrationId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey());
    }

    // 유저 생성 또는 업데이트
    private User saveOrUpdate(OAuthAttributes attributes, String provider) {
        User user = userRepository.findByEmailAndProvider(attributes.getEmail(), provider)
                .map(entity -> entity.update(attributes.getName(), attributes.getPicture()))
                .orElse(attributes.toEntity(provider));

        // 존재하는 사용자면 최근 로그인 시간 업데이트
        if (user.getId() != null) {
            user.setLastLogin(LocalDateTime.now());
        }

        return userRepository.save(user);
    }
}