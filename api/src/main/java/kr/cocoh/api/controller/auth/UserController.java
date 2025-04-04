package kr.cocoh.api.controller.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.cocoh.api.dto.ApiResponse;
import kr.cocoh.api.dto.UserDto;
import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.model.auth.UserActivity;
import kr.cocoh.api.model.auth.enums.Role;
import kr.cocoh.api.security.CustomUserDetails;
import kr.cocoh.api.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Users", description = "사용자 관련 API")
public class UserController {

    private final UserService userService;

    @Operation(summary = "현재 로그인한 사용자 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        User user = userService.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .profileImage(user.getProfileImage())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "사용자 정보 조회 성공", userDto));
    }

    @Operation(summary = "사용자 정보 수정", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/users/me")
    public ResponseEntity<ApiResponse<UserDto>> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        User updatedUser = userService.updateUser(
                userDetails.getId(),
                request.getName(),
                request.getProfileImage()
        );
        
        UserDto userDto = UserDto.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .name(updatedUser.getName())
                .provider(updatedUser.getProvider().name())
                .role(updatedUser.getRole().name())
                .profileImage(updatedUser.getProfileImage())
                .lastLogin(updatedUser.getLastLogin())
                .createdAt(updatedUser.getCreatedAt())
                .build();
        
        // 활동 로그 저장
        Map<String, Object> details = new HashMap<>();
        details.put("name", updatedUser.getName());
        userService.logUserActivity(userDetails.getId(), "user_profile_update", details);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "사용자 정보가 업데이트되었습니다.", userDto));
    }

    @Operation(summary = "비밀번호 변경", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/users/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(
                userDetails.getId(),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        
        // 활동 로그 저장
        userService.logUserActivity(userDetails.getId(), "user_password_change", null);
        
        return ResponseEntity.ok(new ApiResponse<>(true, "비밀번호가 변경되었습니다.", null));
    }

    @Operation(summary = "사용자 활동 로그 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/users/activities")
    public ResponseEntity<ApiResponse<List<UserActivity>>> getUserActivities(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<UserActivity> activities = userService.getUserActivities(userDetails.getId());
        return ResponseEntity.ok(new ApiResponse<>(true, "활동 로그 조회 성공", activities));
    }

    // 관리자용 API
    @Operation(summary = "모든 사용자 목록 조회 (관리자용)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/admin/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userService.getAllUsers();
        return ResponseEntity.ok(new ApiResponse<>(true, "사용자 목록 조회 성공", users));
    }

    @Operation(summary = "사용자 권한 변경 (관리자용)", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/admin/users/{userId}/role")
    @PreAuthorize("hasRole('SUPERADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @Parameter(description = "사용자 ID") @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, "유효하지 않은 권한입니다.", null));
        }
        
        User updatedUser = userService.updateUserRole(userId, role);
        
        UserDto userDto = UserDto.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .name(updatedUser.getName())
                .provider(updatedUser.getProvider().name())
                .role(updatedUser.getRole().name())
                .profileImage(updatedUser.getProfileImage())
                .lastLogin(updatedUser.getLastLogin())
                .createdAt(updatedUser.getCreatedAt())
                .build();
        
        return ResponseEntity.ok(new ApiResponse<>(true, "사용자 권한이 업데이트되었습니다.", userDto));
    }

    // Request DTO classes
    @Data
    public static class UpdateProfileRequest {
        private String name;
        private String profileImage;
    }

    @Data
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

    @Data
    public static class UpdateRoleRequest {
        private String role;
    }
}