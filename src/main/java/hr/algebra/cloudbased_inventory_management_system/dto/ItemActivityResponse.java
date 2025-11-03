package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivityType;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class ItemActivityResponse {
    Long id;
    ItemActivityType type;
    BigDecimal quantityChange;
    String description;
    Instant createdAt;
}
