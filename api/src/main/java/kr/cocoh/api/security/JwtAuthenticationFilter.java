package kr.cocoh.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        try {
            // 요청에서 토큰 추출
            String token = jwtTokenProvider.resolveToken(request);
            
            // 토큰이 유효한 경우 인증 설정
            if (token != null && jwtTokenProvider.validateToken(token)) {
                Authentication auth = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("토큰 인증 성공: {}", token);
            } else if (token != null) {
                log.debug("유효하지 않은 토큰: {}", token);
                
                // 액세스 토큰이 만료된 경우 리프레시 토큰으로 갱신 시도
                String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
                if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
                    // 리프레시 토큰이 유효하면 새 액세스 토큰 발급 로직을 여기에 구현
                    // 이 예제에서는 생략하고 다른 곳에서 구현할 예정
                }
            }
        } catch (Exception e) {
            log.error("인증 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
}