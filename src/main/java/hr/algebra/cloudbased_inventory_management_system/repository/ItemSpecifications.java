package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ItemSpecifications {

    private ItemSpecifications() {
    }

    public static Specification<Item> filterItems(String search, Long supplierId, String category, Boolean lowStock, Boolean activeOnly) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (StringUtils.hasText(search)) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), likePattern),
                        cb.like(cb.lower(root.get("sku")), likePattern)
                ));
            }

            if (supplierId != null) {
                predicates.add(cb.equal(root.get("primarySupplierId"), supplierId));
            }

            if (StringUtils.hasText(category)) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (Boolean.TRUE.equals(lowStock)) {
                predicates.add(cb.and(
                        cb.isNotNull(root.get("currentQty")),
                        cb.isNotNull(root.get("minLevel")),
                        cb.lessThan(root.get("currentQty"), root.get("minLevel"))
                ));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}
