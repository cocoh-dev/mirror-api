package kr.cocoh.api.security.oauth2;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.model.auth.enums.Provider;
import kr.cocoh.api.repository.UserRepository;
import kr.cocoh.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) 
            throws IOException, ServletException {
        if (response.isCommitted()) {
            log.debug("응답이 이미 커밋되었습니다. 리다이렉트할 수 없습니다.");
            return;
        }

        try {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            
            // 사용자 정보 및 제공자 식별
            Map<String, Object> attributes = oAuth2User.getAttributes();
            String email = getEmail(attributes);
            String providerString = getProviderFromRequest(request);

            // 문자열을 Provider enum으로 변환
            Provider providerEnum;
            try {
                providerEnum = Provider.valueOf(providerString.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.error("지원하지 않는 제공자: {}", providerString);
                // 기본값 설정 또는 예외 처리
                providerEnum = Provider.LOCAL; // 또는 적절한 기본값
            }

            // 데이터베이스에서 사용자 조회
            Optional<User> userOptional = userRepository.findByEmailAndProvider(email, providerEnum);
            
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                // 최근 로그인 시간 업데이트
                user.setLastLogin(LocalDateTime.now());
                userRepository.save(user);
                
                // JWT 토큰 생성
                String accessToken = tokenProvider.createAccessToken(user);
                String refreshToken = tokenProvider.createRefreshToken(user);
                
                // 리프레시 토큰 저장
                user.setRefreshToken(refreshToken);
                userRepository.save(user);
                
                // 쿠키에 토큰 설정
                tokenProvider.setCookies(response, accessToken, refreshToken);
                
                // 프론트엔드로 리다이렉트 (토큰을 URL 파라미터로 전달)
                String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("token", accessToken)
                        .queryParam("error", "false")
                        .build().toUriString();
                
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            } else {
                log.error("사용자를 찾을 수 없습니다: {}, {}", email, providerEnum);
                String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                        .queryParam("error", "true")
                        .build().toUriString();
                
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
            }
        } catch (Exception e) {
            log.error("OAuth2 인증 성공 처리 중 오류 발생: {}", e.getMessage());
            throw new ServletException("OAuth2 인증 처리 중 오류가 발생했습니다.", e);
        }
    }
    
    private String getEmail(Map<String, Object> attributes) {
        // Google의 경우 직접 이메일에 접근 가능
        if (attributes.containsKey("email")) {
            return (String) attributes.get("email");
        }
        
        // Kakao의 경우 kakao_account에 이메일이 있음
        if (attributes.containsKey("kakao_account")) {
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (kakaoAccount.containsKey("email")) {
                return (String) kakaoAccount.get("email");
            }
        }
        
        // Naver의 경우 response에 이메일이 있음
        if (attributes.containsKey("response")) {
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            if (response.containsKey("email")) {
                return (String) response.get("email");
            }
        }
        
        return null;
    }
    
    private String getProviderFromRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/oauth2/callback/")) {
            return uri.substring(uri.lastIndexOf("/") + 1);
        }
        return "unknown";
    }
}