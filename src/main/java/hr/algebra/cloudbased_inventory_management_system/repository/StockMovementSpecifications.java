package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.StockMovement;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;

public final class StockMovementSpecifications {

    private StockMovementSpecifications() {
    }

    public static Specification<StockMovement> filter(
            Long itemId,
            MovementType type,
            Instant from,
            Instant to,
            String reason
    ) {
        return Specification.where(byItemId(itemId))
                .and(byType(type))
                .and(createdFrom(from))
                .and(createdTo(to))
                .and(byReason(reason));
    }

    private static Specification<StockMovement> byItemId(Long itemId) {
        return (root, query, builder) -> itemId == null
                ? null
                : builder.equal(root.get("item").get("id"), itemId);
    }

    private static Specification<StockMovement> byType(MovementType type) {
        return (root, query, builder) -> type == null
                ? null
                : builder.equal(root.get("type"), type);
    }

    private static Specification<StockMovement> createdFrom(Instant from) {
        return (root, query, builder) -> from == null
                ? null
                : builder.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    private static Specification<StockMovement> createdTo(Instant to) {
        return (root, query, builder) -> to == null
                ? null
                : builder.lessThanOrEqualTo(root.get("createdAt"), to);
    }

    private static Specification<StockMovement> byReason(String reason) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(reason)) {
                return null;
            }
            String pattern = "%" + reason.trim().toLowerCase() + "%";
            return builder.or(
                    builder.like(builder.lower(root.get("reasonCode")), pattern),
                    builder.like(builder.lower(root.get("note")), pattern)
            );
        };
    }
}

