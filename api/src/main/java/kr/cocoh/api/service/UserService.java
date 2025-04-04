package kr.cocoh.api.service;

import kr.cocoh.api.dto.UserDto;
import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.model.auth.UserActivity;
import kr.cocoh.api.model.auth.enums.Provider;
import kr.cocoh.api.model.auth.enums.Role;
import kr.cocoh.api.repository.UserActivityRepository;
import kr.cocoh.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserActivityRepository userActivityRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일로 사용자 조회
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 이메일과 인증 제공자로 사용자 조회
     */
    public Optional<User> findByEmailAndProvider(String email, String provider) {
        return userRepository.findByEmailAndProvider(email, provider);
    }

    /**
     * ID로 사용자 조회
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * 로컬 회원가입
     */
    @Transactional
    public User registerLocalUser(String email, String name, String password) {
        if (isEmailExists(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .name(name)
                .password(password) // @PrePersist에서 자동 암호화
                .provider(Provider.LOCAL)
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    /**
     * 로그인 처리
     */
    @Transactional
    public User login(String email, String password) {
        User user = userRepository.findByEmailAndProvider(email, "local")
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!user.validatePassword(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 마지막 로그인 시간 업데이트
        user.setLastLogin(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * 사용자 정보 업데이트
     */
    @Transactional
    public User updateUser(Long userId, String name, String profileImage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setName(name);
        if (profileImage != null) {
            user.setProfileImage(profileImage);
        }

        return userRepository.save(user);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!user.validatePassword(currentPassword)) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }

        user.setPassword(newPassword); // @PreUpdate에서 자동 암호화
        userRepository.save(user);
    }

    /**
     * 사용자 권한 변경 (관리자용)
     */
    @Transactional
    public User updateUserRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setRole(role);
        return userRepository.save(user);
    }

    /**
     * 리프레시 토큰 저장
     */
    @Transactional
    public User saveRefreshToken(Long userId, String refreshToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.setRefreshToken(refreshToken);
        return userRepository.save(user);
    }

    /**
     * 사용자 활동 로그 저장
     */
    @Transactional
    public UserActivity logUserActivity(Long userId, String activityType, Map<String, Object> details) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        UserActivity activity = UserActivity.builder()
                .user(user)
                .activityType(activityType)
                .details(details != null ? details.toString() : null)
                .build();

        return userActivityRepository.save(activity);
    }

    /**
     * 사용자 활동 로그 조회
     */
    public List<UserActivity> getUserActivities(Long userId) {
        return userActivityRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 모든 사용자 조회 (관리자용)
     */
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * User 엔티티를 DTO로 변환
     */
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider().name())
                .role(user.getRole().name())
                .profileImage(user.getProfileImage())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .build();
    }
}