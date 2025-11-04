package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.ItemActivityEventType;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemActivityResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemSupplierRequest;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivity;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivityType;
import hr.algebra.cloudbased_inventory_management_system.entity.ItemSupplier;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemActivityQueryRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemActivityRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemSpecifications;
import hr.algebra.cloudbased_inventory_management_system.repository.PurchaseOrderLineRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemActivityRepository itemActivityRepository;
    private final ItemActivityQueryRepository itemActivityQueryRepository;
    private final ItemMapper itemMapper;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final SupplierRepository supplierRepository;

    private static final Set<PurchaseOrderStatus> BLOCKING_PURCHASE_ORDER_STATUSES =
            EnumSet.of(PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.PENDING, PurchaseOrderStatus.PARTIALLY_RECEIVED);

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
        validateNameAndUnit(request.getName(), request.getUnit(), null);
        Item item = itemMapper.toEntity(request);
        if (item.getIsActive() == null) {
            item.setIsActive(Boolean.TRUE);
        }
        assignPrimarySupplier(item, request.getPrimarySupplierId());
        applyAlternateSuppliers(item, request.getAlternateSuppliers());
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
        validateNameAndUnit(request.getName(), request.getUnit(), existing);

        BigDecimal previousQty = existing.getCurrentQty();
        itemMapper.updateEntity(existing, request);
        assignPrimarySupplier(existing, request.getPrimarySupplierId());
        applyAlternateSuppliers(existing, request.getAlternateSuppliers());
        Item saved = itemRepository.save(existing);

        BigDecimal quantityChange = calculateChange(previousQty, saved.getCurrentQty());
        logActivity(saved, ItemActivityType.UPDATED, quantityChange, "Item updated");
        return itemMapper.toResponse(saved);
    }

    @Transactional
    public void deleteItem(Long id) {
        Item existing = itemRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (purchaseOrderLineRepository.existsOpenLinesForItem(id, BLOCKING_PURCHASE_ORDER_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item is referenced by open purchase orders");
        }
        existing.setIsActive(Boolean.FALSE);
        Item saved = itemRepository.save(existing);
        logActivity(saved, ItemActivityType.DELETED, null, "Item deactivated");
    }

    @Transactional(readOnly = true)
    public Page<ItemActivityResponse> getItemActivity(Long itemId, Pageable pageable) {
        if (!itemRepository.existsByIdAndIsActiveTrue(itemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        return itemActivityQueryRepository.findItemActivity(itemId, pageable)
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

    private void validateNameAndUnit(String name, String unit, Item currentItem) {
        String sanitizedName = name != null ? name.trim() : null;
        String sanitizedUnit = unit != null ? unit.trim() : null;
        if (sanitizedName == null || sanitizedName.isEmpty() || sanitizedUnit == null || sanitizedUnit.isEmpty()) {
            return;
        }

        boolean exists;
        if (currentItem == null) {
            exists = itemRepository.existsByNameIgnoreCaseAndUnitIgnoreCaseAndIsActiveTrue(sanitizedName, sanitizedUnit);
        } else {
            exists = itemRepository.existsByNameIgnoreCaseAndUnitIgnoreCaseAndIsActiveTrueAndIdNot(
                    sanitizedName,
                    sanitizedUnit,
                    currentItem.getId()
            );
        }
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Active item with the same name and unit already exists");
        }
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

    private ItemActivityResponse toActivityResponse(ItemActivityQueryRepository.ItemActivityRow row) {
        ItemActivityEventType eventType = determineEventType(row);
        return ItemActivityResponse.builder()
                .id(row.id())
                .eventType(eventType)
                .movementType(row.movementType())
                .quantity(row.quantity())
                .resultingQuantity(row.resultingQuantity())
                .unit(row.unit())
                .reasonCode(row.reasonCode())
                .note(row.note())
                .createdBy(row.createdBy())
                .purchaseOrderId(row.purchaseOrderId())
                .purchaseOrderNumber(row.purchaseOrderNumber())
                .purchaseOrderStatus(row.purchaseOrderStatus())
                .purchaseOrderLineId(row.purchaseOrderLineId())
                .purchaseOrderLineQtyOrdered(row.purchaseOrderLineQtyOrdered())
                .purchaseOrderLineQtyReceived(row.purchaseOrderLineQtyReceived())
                .purchaseOrderLineUnit(row.purchaseOrderLineUnit())
                .itemEventType(row.itemEventType())
                .description(buildDescription(row, eventType))
                .createdAt(row.createdAt())
                .build();
    }

    private ItemActivityEventType determineEventType(ItemActivityQueryRepository.ItemActivityRow row) {
        if (row.source() == ItemActivityQueryRepository.ItemActivitySource.MOVEMENT) {
            return row.purchaseOrderId() != null
                    ? ItemActivityEventType.PURCHASE_ORDER_RECEIPT
                    : ItemActivityEventType.STOCK_MOVEMENT;
        }
        return ItemActivityEventType.ITEM_EVENT;
    }

    private String buildDescription(ItemActivityQueryRepository.ItemActivityRow row, ItemActivityEventType eventType) {
        if (eventType == ItemActivityEventType.ITEM_EVENT) {
            return row.itemEventDescription();
        }
        if (eventType == ItemActivityEventType.PURCHASE_ORDER_RECEIPT) {
            StringBuilder builder = new StringBuilder();
            if (row.quantity() != null) {
                builder.append("Received ").append(row.quantity());
                if (row.unit() != null) {
                    builder.append(' ').append(row.unit());
                }
            }
            if (row.purchaseOrderNumber() != null) {
                if (builder.length() > 0) {
                    builder.append(" from PO ");
                } else {
                    builder.append("PO ");
                }
                builder.append(row.purchaseOrderNumber());
            }
            if (row.purchaseOrderLineId() != null) {
                builder.append(" (Line ").append(row.purchaseOrderLineId()).append(')');
            }
            String composed = builder.toString();
            if (!composed.isEmpty()) {
                return composed;
            }
        }
        if (row.note() != null && !row.note().isBlank()) {
            return row.note();
        }
        return row.reasonCode();
    }

    private void assignPrimarySupplier(Item item, Long supplierId) {
        if (supplierId == null) {
            item.assignPrimarySupplier(null);
            return;
        }
        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary supplier not found"));
        item.assignPrimarySupplier(supplier);
    }

    private void applyAlternateSuppliers(Item item, List<ItemSupplierRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            item.replaceAlternateSuppliers(null);
            return;
        }

        LinkedHashMap<Long, Integer> desired = new LinkedHashMap<>();
        for (ItemSupplierRequest request : requests) {
            if (request == null || request.getSupplierId() == null) {
                continue;
            }
            Long supplierId = request.getSupplierId();
            if (Objects.equals(item.getPrimarySupplierId(), supplierId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alternate supplier cannot match primary supplier");
            }
            if (desired.putIfAbsent(supplierId, request.getPriority()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate alternate supplier: " + supplierId);
            }
        }

        if (desired.isEmpty()) {
            item.replaceAlternateSuppliers(null);
            return;
        }

        List<Supplier> suppliers = supplierRepository.findByIdInAndIsActiveTrue(desired.keySet());
        if (suppliers.size() != desired.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more alternate suppliers are invalid or inactive");
        }

        Map<Long, Supplier> supplierIndex = suppliers.stream()
                .collect(Collectors.toMap(Supplier::getId, supplier -> supplier));

        int fallbackPriority = 0;
        Set<Integer> usedPriorities = new HashSet<>();
        List<ItemSupplier> alternates = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : desired.entrySet()) {
            Supplier supplier = supplierIndex.get(entry.getKey());
            if (supplier == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supplier not found: " + entry.getKey());
            }
            Integer priority = entry.getValue();
            if (priority != null && priority < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Priority must be zero or positive");
            }
            if (priority == null) {
                while (usedPriorities.contains(fallbackPriority)) {
                    fallbackPriority++;
                }
                priority = fallbackPriority;
                fallbackPriority++;
                usedPriorities.add(priority);
            } else {
                if (!usedPriorities.add(priority)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate priority value: " + priority);
                }
            }
            ItemSupplier alternate = ItemSupplier.builder()
                    .supplier(supplier)
                    .priority(priority)
                    .build();
            alternates.add(alternate);
        }
        item.replaceAlternateSuppliers(alternates);
    }
}
