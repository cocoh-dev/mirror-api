package kr.cocoh.api.model.auth;

import kr.cocoh.api.model.ad.Ad;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "salons")
public class Salon {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(name = "business_hours", nullable = false, length = 100)
    private String businessHours;
    
    @Column(name = "business_number", nullable = false, unique = true, length = 10)
    private String businessNumber;
    
    @Column(nullable = false, length = 20)
    private String phone;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @OneToOne(mappedBy = "salon", cascade = CascadeType.ALL, orphanRemoval = true)
    private Location location;
    
    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL)
    private List<Display> displays;
    
    @OneToMany(mappedBy = "salon", cascade = CascadeType.ALL)
    private List<Ad> ads;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enum for status
    public enum Status {
        pending, approved, rejected
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = Status.pending;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}