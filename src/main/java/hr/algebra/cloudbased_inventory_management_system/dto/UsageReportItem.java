package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class UsageReportItem {
    private final Long itemId;
    private final String sku;
    private final String name;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;
    private final BigDecimal netUsage;

    public UsageReportItem(Long itemId, String sku, String name, BigDecimal totalIn, BigDecimal totalOut) {
        this.itemId = itemId;
        this.sku = sku;
        this.name = name;
        this.totalIn = normalize(totalIn);
        this.totalOut = normalize(totalOut);
        this.netUsage = this.totalOut.subtract(this.totalIn);
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal normalized = value.stripTrailingZeros();
        if (BigDecimal.ZERO.compareTo(normalized) == 0) {
            return BigDecimal.ZERO;
        }
        return normalized;
    }
}
