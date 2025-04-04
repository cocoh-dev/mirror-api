package kr.cocoh.api.model.pay;

import kr.cocoh.api.model.auth.User;
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
@Table(name = "payments")
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;
    
    @Column(nullable = false)
    private Integer amount;
    
    @Column(nullable = false, length = 3)
    private String currency;
    
    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "merchant_uid", nullable = false, unique = true, length = 100)
    private String merchantUid;
    
    @Column(name = "pg_provider", nullable = false, length = 50)
    private String pgProvider;
    
    @Column(name = "receipt_url", length = 255)
    private String receiptUrl;
    
    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL)
    private List<PaymentRefund> refunds;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Enum for payment status
    public enum PaymentStatus {
        pending, completed, failed, refunded
    }
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.pending;
        }
        if (this.currency == null) {
            this.currency = "KRW";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}