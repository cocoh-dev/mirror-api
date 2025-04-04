package kr.cocoh.api.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.cocoh.api.dto.ApiResponse;
import kr.cocoh.api.dto.UserDto;
import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.security.JwtTokenProvider;
import kr.cocoh.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserDto>> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = userService.registerLocalUser(
                    request.getEmail(),
                    request.getName(),
                    request.getPassword()
            );
            
            UserDto userDto = UserDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .provider(user.getProvider().name())
                    .role(user.getRole().name())
                    .createdAt(user.getCreatedAt())
                    .build();
            
            // 활동 로그 저장
            Map<String, Object> details = new HashMap<>();
            details.put("email", user.getEmail());
            userService.logUserActivity(user.getId(), "user_register", details);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "회원가입이 완료되었습니다.", userDto));
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        try {
            User user = userService.login(request.getEmail(), request.getPassword());
            
            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createAccessToken(user);
            String refreshToken = jwtTokenProvider.createRefreshToken(user);
            
            // 리프레시 토큰 저장
            userService.saveRefreshToken(user.getId(), refreshToken);
            
            // 쿠키에 토큰 설정
            jwtTokenProvider.setCookies(response, accessToken, refreshToken);
            
            // 활동 로그 저장
            Map<String, Object> details = new HashMap<>();
            details.put("ip", request.getIp());
            details.put("userAgent", request.getUserAgent());
            userService.logUserActivity(user.getId(), "user_login", details);
            
            LoginResponse loginResponse = new LoginResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole().name(),
                    user.getProfileImage(),
                    accessToken
            );
            
            return ResponseEntity.ok(new ApiResponse<>(true, "로그인 성공", loginResponse));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String bearerToken,
            HttpServletResponse response) {
        
        // 토큰에서 사용자 ID 추출
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            try {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                
                // 리프레시 토큰 제거
                userService.saveRefreshToken(userId, null);
                
                // 활동 로그 저장
                userService.logUserActivity(userId, "user_logout", null);
                
            } catch (Exception e) {
                log.error("로그아웃 처리 중 오류: {}", e.getMessage());
            }
        }
        
        // 쿠키 제거
        jwtTokenProvider.clearCookies(response);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "로그아웃 되었습니다.", null));
    }

    // Request/Response DTO classes
    @Data
    public static class SignupRequest {
        private String email;
        private String name;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String email;
        private String password;
        private String ip;
        private String userAgent;
    }

    @Data
    @AllArgsConstructor
    public static class LoginResponse {
        private Long id;
        private String email;
        private String name;
        private String role;
        private String profileImage;
        private String accessToken;
    }
}
