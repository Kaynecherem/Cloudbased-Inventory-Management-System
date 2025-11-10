package hr.algebra.cloudbased_inventory_management_system.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TopMoverItem {
    private final Long itemId;
    private final String sku;
    private final String name;
    private final BigDecimal totalIn;
    private final BigDecimal totalOut;
    private final BigDecimal totalMovement;

    public TopMoverItem(
            Long itemId,
            String sku,
            String name,
            BigDecimal totalIn,
            BigDecimal totalOut
    ) {
        this.itemId = itemId;
        this.sku = sku;
        this.name = name;
        this.totalIn = normalize(totalIn);
        this.totalOut = normalize(totalOut);
        this.totalMovement = normalize(this.totalIn.add(this.totalOut));
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
