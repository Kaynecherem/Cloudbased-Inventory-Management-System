package hr.algebra.cloudbased_inventory_management_system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String sku;

    private String category;

    @NotBlank
    private String unit;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal currentQty;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal minLevel;

    private Long locationId;

    private Long primarySupplierId;

    private Boolean isActive;

    private List<@Valid ItemSupplierRequest> alternateSuppliers;
}
