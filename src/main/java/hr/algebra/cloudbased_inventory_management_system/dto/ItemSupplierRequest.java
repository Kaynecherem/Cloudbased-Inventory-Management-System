package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemSupplierRequest {

    @NotNull
    private Long supplierId;

    @Min(0)
    private Integer priority;
}

