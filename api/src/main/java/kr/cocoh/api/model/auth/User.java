package kr.cocoh.api.model.auth;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import kr.cocoh.api.model.auth.enums.Provider;
import kr.cocoh.api.model.auth.enums.Role;
import kr.cocoh.api.model.pay.Payment;
import kr.cocoh.api.model.pay.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(length = 255)
    private String password;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    
    @Column(name = "provider_id", length = 255)
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    
    @Column(name = "profile_image", length = 255)
    private String profileImage;
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserActivity> activities;
    
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Salon> ownedSalons;
    
    @OneToMany(mappedBy = "user")
    private List<Subscription> subscriptions;
    
    @OneToMany(mappedBy = "user")
    private List<Payment> payments;
    
    // Enum 정의는 kr.cocoh.api.model.auth.enums 패키지로 이동됨
    
    // Method to validate password
    public boolean validatePassword(String password) {
        if (this.password == null) return false;
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        return encoder.matches(password, this.password);
    }
    
    // JPA lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.encodePassword();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.encodePassword();
    }
    
    public User update(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this;
    }
    
    // Password encoding
    private void encodePassword() {
        if (this.password != null && (this.id == null || this.provider == Provider.LOCAL)) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            this.password = encoder.encode(this.password);
        }
    }
}