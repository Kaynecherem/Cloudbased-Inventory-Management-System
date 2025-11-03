package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class PurchaseOrderResponse {
    Long id;
    String number;
    Long supplierId;
    String supplierName;
    PurchaseOrderStatus status;
    Instant eta;
    Long createdById;
    String createdByName;
    Instant createdAt;
    Instant updatedAt;
    List<PurchaseOrderLineResponse> lines;
}
