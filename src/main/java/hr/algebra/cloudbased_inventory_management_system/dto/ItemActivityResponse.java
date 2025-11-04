package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivityType;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class ItemActivityResponse {
    Long id;
    ItemActivityEventType eventType;
    MovementType movementType;
    BigDecimal quantity;
    BigDecimal resultingQuantity;
    String unit;
    String reasonCode;
    String note;
    String createdBy;
    Long purchaseOrderId;
    String purchaseOrderNumber;
    PurchaseOrderStatus purchaseOrderStatus;
    Long purchaseOrderLineId;
    BigDecimal purchaseOrderLineQtyOrdered;
    BigDecimal purchaseOrderLineQtyReceived;
    String purchaseOrderLineUnit;
    ItemActivityType itemEventType;
    String description;
    Instant createdAt;
}
