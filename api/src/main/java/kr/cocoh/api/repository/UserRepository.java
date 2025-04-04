package kr.cocoh.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import kr.cocoh.api.model.auth.User;
import kr.cocoh.api.model.auth.enums.Provider;
import kr.cocoh.api.model.auth.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndProvider(String email, Provider provider);
    
    Optional<User> findByRefreshToken(String refreshToken);
    
    List<User> findByRole(Role role);
    
    @Query("SELECT u FROM User u WHERE u.lastLogin >= :startDate")
    List<User> findActiveUsersSince(@Param("startDate") LocalDateTime startDate);
    
    @Query("SELECT u FROM User u WHERE " +
            "(LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:role IS NULL OR u.role = :role) " +
            "AND (:provider IS NULL OR u.provider = :provider)")
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("role") Role role,
            @Param("provider") Provider provider,
            Pageable pageable);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countNewUsersSince(@Param("startDate") LocalDateTime startDate);
    
    boolean existsByEmail(String email);
}