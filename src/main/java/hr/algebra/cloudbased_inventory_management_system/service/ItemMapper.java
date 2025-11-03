package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.ItemRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

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
        item.setPrimarySupplierId(request.getPrimarySupplierId());
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
                .build();
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros();
    }
}
