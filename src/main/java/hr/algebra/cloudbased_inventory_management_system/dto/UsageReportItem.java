package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class UsageReportItem {
    private final Long itemId;
    private final String sku;
    private final String name;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;
    private final BigDecimal netUsage;
    private final BigDecimal averageOutPerDay;

    public UsageReportItem(
            Long itemId,
            String sku,
            String name,
            BigDecimal totalIn,
            BigDecimal totalOut,
            BigDecimal windowDays
    ) {
        this.itemId = itemId;
        this.sku = sku;
        this.name = name;
        this.totalIn = normalize(totalIn);
        this.totalOut = normalize(totalOut);
        this.netUsage = this.totalOut.subtract(this.totalIn);
        this.averageOutPerDay = calculateAverage(this.totalOut, windowDays);
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

    private BigDecimal calculateAverage(BigDecimal totalOut, BigDecimal windowDays) {
        if (totalOut == null || BigDecimal.ZERO.compareTo(totalOut) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal effectiveWindow = windowDays;
        if (effectiveWindow == null || effectiveWindow.compareTo(BigDecimal.ONE) < 0) {
            effectiveWindow = BigDecimal.ONE;
        }
        return totalOut.divide(effectiveWindow, 2, RoundingMode.HALF_UP);
    }
}
