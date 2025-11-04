package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_lines",
        indexes = {
                @Index(name = "idx_pol_po", columnList = "po_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "qty_ordered", precision = 19, scale = 2, nullable = false)
    private BigDecimal qtyOrdered;

    @Column(name = "qty_received", precision = 19, scale = 2, nullable = false)
    private BigDecimal qtyReceived;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Version
    @Column(nullable = false)
    private Long version;
}
