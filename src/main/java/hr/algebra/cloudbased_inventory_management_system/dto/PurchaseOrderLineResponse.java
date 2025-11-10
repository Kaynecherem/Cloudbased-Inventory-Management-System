package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PurchaseOrderLineResponse {
    Long id;
    Long itemId;
    String itemName;
    BigDecimal qtyOrdered;
    BigDecimal qtyReceived;
    String unit;
    BigDecimal price;
    String createdBy;
    String updatedBy;
}
