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
                @Index(name = "ux_stock_movements_client_request_id", columnList = "client_request_id", unique = true),
                @Index(name = "idx_stock_movements_item_created_at", columnList = "item_id, created_at")
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
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_stock_movements_item",
                    foreignKeyDefinition = "FOREIGN KEY (item_id) REFERENCES items(id) ON UPDATE RESTRICT ON DELETE RESTRICT"))
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

    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id",
            foreignKey = @ForeignKey(name = "fk_stock_movements_po",
                    foreignKeyDefinition = "FOREIGN KEY (po_id) REFERENCES purchase_orders(id) ON UPDATE RESTRICT ON DELETE RESTRICT"))
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id",
            foreignKey = @ForeignKey(name = "fk_stock_movements_po_line",
                    foreignKeyDefinition = "FOREIGN KEY (po_line_id) REFERENCES purchase_order_lines(id) ON UPDATE RESTRICT ON DELETE RESTRICT"))
    private PurchaseOrderLine purchaseOrderLine;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (updatedBy == null) {
            updatedBy = createdBy != null ? createdBy : "system";
        }
    }

    @PreUpdate
    void onUpdate() {
        if (updatedBy == null) {
            updatedBy = createdBy != null ? createdBy : "system";
        }
    }
}

