package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ItemSupplierId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;
}

