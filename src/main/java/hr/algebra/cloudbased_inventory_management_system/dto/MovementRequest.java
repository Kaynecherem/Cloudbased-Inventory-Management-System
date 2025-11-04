package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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
public class MovementRequest {

    @NotNull
    private Long itemId;

    @NotNull
    private MovementType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal qty;

    @NotBlank
    private String unit;

    @Size(max = 100)
    private String reasonCode;

    @Size(max = 500)
    private String note;

    @Size(max = 100)
    private String clientRequestId;

    private Long purchaseOrderId;

    private Long purchaseOrderLineId;
}

