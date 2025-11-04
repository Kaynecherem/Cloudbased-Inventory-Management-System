package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "stock_movements",
        indexes = {
                @Index(name = "ux_stock_movements_client_request_id", columnList = "client_request_id", unique = true)
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementType type;

    @Column(name = "quantity", precision = 19, scale = 2, nullable = false)
    private BigDecimal quantity;

    @Column(name = "resulting_quantity", precision = 19, scale = 2, nullable = false)
    private BigDecimal resultingQuantity;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(name = "reason_code", length = 100)
    private String reasonCode;

    @Column(length = 500)
    private String note;

    @Column(name = "client_request_id", length = 100)
    private String clientRequestId;

    @Column(nullable = false, length = 150, updatable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}

