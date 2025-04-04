package kr.cocoh.api.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.cocoh.api.model.auth.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.refresh-token.secret}")
    private String refreshSecretKey;
    
    @Value("${jwt.expiration}")
    private long accessTokenValidity;  // 1시간

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenValidity; // 7일

    private Key key;
    private Key refreshKey;

    @PostConstruct
    protected void init() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(keyBytes));
        
        byte[] refreshKeyBytes = refreshSecretKey.getBytes(StandardCharsets.UTF_8);
        this.refreshKey = Keys.hmacShaKeyFor(Base64.getEncoder().encode(refreshKeyBytes));
    }

    // 액세스 토큰 생성
    public String createAccessToken(User user) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(user.getId()));
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().name());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 리프레시 토큰 생성
    public String createRefreshToken(User user) {
        Claims claims = Jwts.claims().setSubject(String.valueOf(user.getId()));
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenValidity);
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(refreshKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 쿠키에 토큰 설정
    public void setCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        boolean isProduction = System.getProperty("spring.profiles.active", "").equals("production");
        String cookieDomain = isProduction ? System.getenv("COOKIE_DOMAIN") : null;
        
        Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isProduction);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge((int) (accessTokenValidity / 1000)); // 초 단위로 변환
        
        if (cookieDomain != null) {
            accessTokenCookie.setDomain(cookieDomain);
        }
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isProduction);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge((int) (refreshTokenValidity / 1000)); // 초 단위로 변환
        
        if (cookieDomain != null) {
            refreshTokenCookie.setDomain(cookieDomain);
        }
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    // 쿠키 제거
    public void clearCookies(HttpServletResponse response) {
        boolean isProduction = System.getProperty("spring.profiles.active", "").equals("production");
        String cookieDomain = isProduction ? System.getenv("COOKIE_DOMAIN") : null;
        
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(isProduction);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0);
        
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(isProduction);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        
        if (cookieDomain != null) {
            accessTokenCookie.setDomain(cookieDomain);
            refreshTokenCookie.setDomain(cookieDomain);
        }
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }

    // 요청에서 토큰 추출
    public String resolveToken(HttpServletRequest request) {
        // 헤더에서 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 쿠키에서 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }

    // 리프레시 토큰 추출
    public String resolveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    // 토큰 검증
    public boolean validateRefreshToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder()
                    .setSigningKey(refreshKey)
                    .build()
                    .parseClaimsJws(token);
            
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    // 토큰에서 사용자 ID 추출
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return Long.parseLong(claims.getSubject());
    }

    // 토큰에서 사용자 이메일 추출
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("email", String.class);
    }

    // 토큰에서 사용자 권한 추출
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        Long id = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        
        Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        
        CustomUserDetails principal = new CustomUserDetails(id, email, "", authorities);
        
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }
}