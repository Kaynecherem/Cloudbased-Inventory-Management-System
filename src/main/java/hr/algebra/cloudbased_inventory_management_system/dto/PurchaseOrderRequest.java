package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderRequest {

    @NotNull
    private Long supplierId;

    private Instant eta;

    @Valid
    @Size(min = 1, max = 200)
    private List<PurchaseOrderLineRequest> lines;
}
