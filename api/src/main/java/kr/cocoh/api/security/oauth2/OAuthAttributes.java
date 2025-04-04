package kr.cocoh.api.security.oauth2;

import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.model.auth.enums.Role;
import kr.cocoh.api.model.auth.enums.Provider;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private String providerId;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, 
                            String name, String email, String picture, String providerId) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.providerId = providerId;
    }

    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        log.debug("OAuth Provider: {}", registrationId);
        
        if ("kakao".equals(registrationId)) {
            return ofKakao(userNameAttributeName, attributes);
        } else if ("naver".equals(registrationId)) {
            return ofNaver(userNameAttributeName, attributes);
        }
        
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .providerId((String) attributes.get("sub"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        // 카카오는 kakao_account에 유저정보가 있음
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> kakaoProfile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuthAttributes.builder()
                .name((String) kakaoProfile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .picture((String) kakaoProfile.get("profile_image_url"))
                .providerId(String.valueOf(attributes.get("id")))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        // 네이버는 response에 유저정보가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .providerId((String) response.get("id"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity(String providerStr) {
        Provider provider;
        try {
            provider = Provider.valueOf(providerStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 예외 처리: 알 수 없는 provider인 경우 기본값 설정
            provider = Provider.LOCAL;
        }
        
        return User.builder()
                .name(name)
                .email(email != null ? email : providerStr.toLowerCase() + "_" + providerId + "@example.com")
                .profileImage(picture)
                .provider(provider)
                .providerId(providerId)
                .role(Role.USER)  // 별도 파일로 분리된 Role 열거형 사용
                .build();
    }
}
