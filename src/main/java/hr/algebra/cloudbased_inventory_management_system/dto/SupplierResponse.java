package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SupplierResponse {
    Long id;
    String name;
    String contactName;
    String email;
    String phone;
    Integer leadTimeDays;
    String address;
    String notes;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
}
