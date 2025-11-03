package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineRequest {

    @NotNull
    private Long itemId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal qtyOrdered;

    @NotNull
    @Size(min = 1, max = 50)
    private String unit;

    private BigDecimal price;
}
