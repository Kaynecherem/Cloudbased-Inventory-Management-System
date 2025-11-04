package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = "items",
        indexes = {
                @Index(name = "idx_items_low", columnList = "current_qty, min_level")
        }
)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(nullable = false)
    private String unit;

    @Column(name = "current_qty", precision = 19, scale = 2, nullable = false)
    private BigDecimal currentQty;

    @Column(name = "min_level", precision = 19, scale = 2)
    private BigDecimal minLevel;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "primary_supplier_id")
    private Long primarySupplierId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_supplier_id", foreignKey = @ForeignKey(name = "fk_items_primary_supplier"), insertable = false, updatable = false)
    private Supplier primarySupplier;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("priority ASC, supplier.id ASC")
    @Builder.Default
    private List<ItemSupplier> alternateSuppliers = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public void assignPrimarySupplier(Supplier supplier) {
        this.primarySupplier = supplier;
        this.primarySupplierId = supplier != null ? supplier.getId() : null;
    }

    public void replaceAlternateSuppliers(Collection<ItemSupplier> alternates) {
        if (alternateSuppliers == null) {
            alternateSuppliers = new ArrayList<>();
        }
        alternateSuppliers.forEach(alternate -> alternate.setItem(null));
        alternateSuppliers.clear();
        if (alternates == null || alternates.isEmpty()) {
            return;
        }
        alternates.forEach(this::addAlternateSupplier);
    }

    public void addAlternateSupplier(ItemSupplier alternateSupplier) {
        if (alternateSupplier == null) {
            return;
        }
        Supplier supplier = alternateSupplier.getSupplier();
        if (supplier != null) {
            alternateSupplier.setSupplier(supplier);
        }
        alternateSupplier.setItem(this);
        alternateSuppliers.add(alternateSupplier);
    }
}
