package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item_suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_suppliers_item_supplier", columnNames = {"item_id", "supplier_id"})
        },
        indexes = {
                @Index(name = "idx_item_suppliers_item_priority", columnList = "item_id, priority")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSupplier {

    @EmbeddedId
    @Builder.Default
    private ItemSupplierId id = new ItemSupplierId();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("itemId")
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_suppliers_item"))
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("supplierId")
    @JoinColumn(name = "supplier_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_suppliers_supplier"))
    private Supplier supplier;

    @Column(nullable = false)
    private Integer priority;

    public void setItem(Item item) {
        this.item = item;
        if (this.id == null) {
            this.id = new ItemSupplierId();
        }
        this.id.setItemId(item != null ? item.getId() : null);
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
        if (this.id == null) {
            this.id = new ItemSupplierId();
        }
        this.id.setSupplierId(supplier != null ? supplier.getId() : null);
    }

    @PrePersist
    @PreUpdate
    void syncIds() {
        if (this.id == null) {
            this.id = new ItemSupplierId();
        }
        if (this.item != null) {
            this.id.setItemId(this.item.getId());
        }
        if (this.supplier != null) {
            this.id.setSupplierId(this.supplier.getId());
        }
    }
}

