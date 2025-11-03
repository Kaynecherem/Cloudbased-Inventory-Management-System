package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.ItemActivityResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivity;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivityType;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemActivityRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemActivityRepository itemActivityRepository;
    private final ItemMapper itemMapper;

    @Transactional(readOnly = true)
    public Page<ItemResponse> findItems(String search, Long supplierId, String category, Boolean lowStock, Pageable pageable) {
        return itemRepository.findAll(
                ItemSpecifications.filterItems(search, supplierId, category, lowStock, Boolean.TRUE),
                pageable
        ).map(itemMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItem(Long id) {
        Item item = itemRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        return itemMapper.toResponse(item);
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        validateSku(request.getSku(), null);
        Item item = itemMapper.toEntity(request);
        if (item.getIsActive() == null) {
            item.setIsActive(Boolean.TRUE);
        }
        Item saved = itemRepository.save(item);
        logActivity(saved, ItemActivityType.CREATED, saved.getCurrentQty(), "Item created");
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item existing = itemRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        if (!existing.getSku().equalsIgnoreCase(request.getSku())) {
            validateSku(request.getSku(), existing);
        }

        BigDecimal previousQty = existing.getCurrentQty();
        itemMapper.updateEntity(existing, request);
        Item saved = itemRepository.save(existing);

        BigDecimal quantityChange = calculateChange(previousQty, saved.getCurrentQty());
        logActivity(saved, ItemActivityType.UPDATED, quantityChange, "Item updated");
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public void deleteItem(Long id) {
        Item existing = itemRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        existing.setIsActive(Boolean.FALSE);
        Item saved = itemRepository.save(existing);
        logActivity(saved, ItemActivityType.DELETED, null, "Item deactivated");
    }

    @Transactional(readOnly = true)
    public Page<ItemActivityResponse> getItemActivity(Long itemId, Pageable pageable) {
        if (!itemRepository.existsByIdAndIsActiveTrue(itemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        return itemActivityRepository.findByItemIdOrderByCreatedAtDesc(itemId, pageable)
                .map(this::toActivityResponse);
    }

    private void validateSku(String sku, Item currentItem) {
        String sanitizedSku = sku != null ? sku.trim() : null;
        if (sanitizedSku == null || sanitizedSku.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SKU is required");
        }
        boolean exists = itemRepository.existsBySkuIgnoreCase(sanitizedSku);
        if (!exists) {
            return;
        }
        if (currentItem != null && sanitizedSku.equalsIgnoreCase(currentItem.getSku())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "SKU already exists");
    }

    private void logActivity(Item item, ItemActivityType type, BigDecimal quantityChange, String description) {
        ItemActivity activity = ItemActivity.builder()
                .item(item)
                .type(type)
                .quantityChange(quantityChange == null || BigDecimal.ZERO.compareTo(quantityChange) == 0 ? null : quantityChange)
                .description(description)
                .build();
        itemActivityRepository.save(activity);
    }

    private BigDecimal calculateChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null) {
            return null;
        }
        BigDecimal change = current.subtract(previous);
        return BigDecimal.ZERO.compareTo(change) == 0 ? null : change;
    }

    private ItemActivityResponse toActivityResponse(ItemActivity activity) {
        return ItemActivityResponse.builder()
                .id(activity.getId())
                .type(activity.getType())
                .quantityChange(activity.getQuantityChange())
                .description(activity.getDescription())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}
