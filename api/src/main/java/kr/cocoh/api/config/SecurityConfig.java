package kr.cocoh.api.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import kr.cocoh.api.security.JwtAuthenticationFilter;
import kr.cocoh.api.security.JwtTokenProvider;
import kr.cocoh.api.security.oauth2.CustomOAuth2UserService;
import kr.cocoh.api.security.oauth2.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보호 비활성화 (REST API로 사용)
            .csrf(AbstractHttpConfigurer::disable)
            // CORS 설정 활성화
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 세션 관리 정책 설정: STATELESS로 세션을 생성하지 않음
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 요청에 대한 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers("/auth/**", "/api/public/**", "/api-docs/**", "/swagger-ui/**").permitAll()
                // 관리자 전용 엔드포인트
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                // 슈퍼 관리자 전용 엔드포인트
                .requestMatchers("/api/superadmin/**").hasRole("SUPERADMIN")
                // 나머지 요청은 인증 필요
                .anyRequest().authenticated()
            )
            // OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            // JWT 필터 추가
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 프로덕션 환경에서는 실제 도메인으로 설정
        String frontendUrl = System.getenv("FRONTEND_URL");
        List<String> allowedOrigins = frontendUrl != null ?
                List.of(frontendUrl, "https://app.cocoh.kr", "https://test-app.cocoh.kr") :
                List.of("http://localhost:3000", "http://localhost:5173");
                
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Content-Range", "X-Content-Range"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}