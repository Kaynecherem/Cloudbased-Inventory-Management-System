package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.LowStockAlertResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemSpecifications;
import hr.algebra.cloudbased_inventory_management_system.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getLowStockAlerts() {
        List<Item> lowStockItems = itemRepository.findAll(
                        ItemSpecifications.filterItems(null, null, null, true, true),
                        Sort.by(Sort.Direction.ASC, "name")
                );

        Set<Long> supplierIds = lowStockItems.stream()
                .map(Item::getPrimarySupplierId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, Supplier> suppliersById = supplierIds.isEmpty()
                ? Map.of()
                : supplierRepository.findByIdInAndIsActiveTrue(supplierIds)
                .stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));

        return lowStockItems.stream()
                .map(item -> toLowStockResponse(item, suppliersById.get(item.getPrimarySupplierId())))
                .toList();
    }

    private LowStockAlertResponse toLowStockResponse(Item item, Supplier supplier) {
        BigDecimal minLevel = normalize(item.getMinLevel());
        BigDecimal currentQty = normalize(item.getCurrentQty());
        BigDecimal shortage = minLevel.subtract(currentQty);
        BigDecimal suggestedOrderQty = shortage.max(BigDecimal.ZERO);

        return LowStockAlertResponse.builder()
                .itemId(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .category(item.getCategory())
                .currentQty(currentQty)
                .minLevel(minLevel)
                .shortage(shortage.max(BigDecimal.ZERO))
                .supplierId(supplier != null ? supplier.getId() : null)
                .supplierName(supplier != null ? supplier.getName() : null)
                .supplierEmail(supplier != null ? supplier.getEmail() : null)
                .supplierPhone(supplier != null ? supplier.getPhone() : null)
                .supplierLeadTimeDays(supplier != null ? supplier.getLeadTimeDays() : null)
                .hasPrimarySupplier(supplier != null)
                .suggestedOrderQty(suggestedOrderQty)
                .build();
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
