package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiveRequest {

    @NotNull
    private Long lineId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal receivedQty;
}
