package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.ItemRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemSupplierResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemSupplier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class ItemMapper {

    public Item toEntity(ItemRequest request) {
        Item item = new Item();
        updateEntity(item, request);
        return item;
    }

    public void updateEntity(Item item, ItemRequest request) {
        item.setName(trimOrNull(request.getName()));
        item.setSku(trimOrNull(request.getSku()));
        item.setCategory(trimOrNull(request.getCategory()));
        item.setUnit(trimOrNull(request.getUnit()));
        item.setCurrentQty(normalizeDecimal(request.getCurrentQty()));
        item.setMinLevel(normalizeDecimal(request.getMinLevel()));
        item.setLocationId(request.getLocationId());
        if (request.getIsActive() != null) {
            item.setIsActive(request.getIsActive());
        }
    }

    public ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .sku(item.getSku())
                .category(item.getCategory())
                .unit(item.getUnit())
                .currentQty(item.getCurrentQty())
                .minLevel(item.getMinLevel())
                .locationId(item.getLocationId())
                .primarySupplierId(item.getPrimarySupplierId())
                .isActive(item.getIsActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .alternateSuppliers(mapAlternateSuppliers(item))
                .build();
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }

    private List<ItemSupplierResponse> mapAlternateSuppliers(Item item) {
        List<ItemSupplier> alternates = item.getAlternateSuppliers();
        if (alternates == null || alternates.isEmpty()) {
            return List.of();
        }
        return alternates.stream()
                .filter(alternate -> alternate.getSupplier() != null)
                .sorted(Comparator.comparing(ItemSupplier::getPriority, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(alternate -> alternate.getSupplier().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(alternate -> ItemSupplierResponse.builder()
                        .supplierId(alternate.getSupplier().getId())
                        .supplierName(alternate.getSupplier().getName())
                        .priority(alternate.getPriority())
                        .build())
                .toList();
    }
}
