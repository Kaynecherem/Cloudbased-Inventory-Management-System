package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ItemSupplierResponse {
    Long supplierId;
    String supplierName;
    Integer priority;
}

