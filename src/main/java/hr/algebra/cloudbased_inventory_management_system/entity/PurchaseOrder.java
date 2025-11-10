package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchase_orders_number", columnNames = "number")
        },
        indexes = {
                @Index(name = "idx_po_supplier_status", columnList = "supplier_id,status")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseOrderStatus status;

    @Column(name = "eta")
    private Instant eta;

    @Column(name = "created_by", nullable = false, length = 150, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = PurchaseOrderStatus.DRAFT;
        }
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
        if (this.updatedBy == null) {
            this.updatedBy = this.createdBy;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        if (this.updatedBy == null) {
            this.updatedBy = this.createdBy != null ? this.createdBy : "system";
        }
    }

    public void addLine(PurchaseOrderLine line) {
        this.lines.add(line);
        line.setPurchaseOrder(this);
    }

    public void clearLines() {
        if (this.lines != null) {
            this.lines.forEach(line -> line.setPurchaseOrder(null));
            this.lines.clear();
        }
    }
}
