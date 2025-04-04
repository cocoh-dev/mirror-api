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
@Table(name = "ad_medias")
public class AdMedia {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ad_id", nullable = false)
    private Ad ad;
    
    @Column(nullable = false, length = 255)
    private String url;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType type;
    
    @Column(nullable = false)
    private Integer order;
    
    @Column(nullable = false)
    private Integer duration;
    
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enum for media type
    public enum MediaType {
        image, video
    }
    
    // Enum for size
    public enum Size {
        min, max
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.order == null) {
            this.order = 0;
        }
        if (this.duration == null) {
            this.duration = 30;
        }
        if (this.isPrimary == null) {
            this.isPrimary = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}