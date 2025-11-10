package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
@Builder
public class ItemResponse {
    Long id;
    String name;
    String sku;
    String category;
    String unit;
    BigDecimal currentQty;
    BigDecimal minLevel;
    Long locationId;
    Long primarySupplierId;
    Boolean isActive;
    String createdBy;
    String updatedBy;
    Instant createdAt;
    Instant updatedAt;
    List<ItemSupplierResponse> alternateSuppliers;
}
