package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class LowStockAlertResponse {
    Long itemId;
    String sku;
    String name;
    String category;
    BigDecimal currentQty;
    BigDecimal minLevel;
    BigDecimal shortage;
    Long supplierId;
    String supplierName;
    String supplierEmail;
    String supplierPhone;
    Integer supplierLeadTimeDays;
    boolean hasPrimarySupplier;
    BigDecimal suggestedOrderQty;
}
