package kr.cocoh.api.model.ad;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ad_locations")
public class AdLocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private TargetType targetType;
    
    @Column(length = 100)
    private String city;
    
    @Column(length = 100)
    private String district;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enum for target type
    public enum TargetType {
        nationwide, administrative
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Validation methods
    @PrePersist
    @PreUpdate
    protected void validate() {
        if (this.targetType == TargetType.nationwide) {
            if (this.city != null || this.district != null) {
                throw new IllegalStateException("Nationwide targeting cannot have other location fields");
            }
        }
        
        if (this.targetType == TargetType.administrative) {
            if (this.city == null) {
                throw new IllegalStateException("City is required for administrative targeting");
            }
        }
    }
}