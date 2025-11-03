package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class MovementResponse {
    Long id;
    Long itemId;
    String itemName;
    MovementType type;
    BigDecimal qty;
    BigDecimal resultingQty;
    String unit;
    String reasonCode;
    String note;
    Instant createdAt;
}

