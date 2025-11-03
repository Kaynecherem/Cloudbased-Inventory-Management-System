package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.LowStockAlertResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getLowStockAlerts() {
        return itemRepository.findAll(
                        ItemSpecifications.filterItems(null, null, null, true, true),
                        Sort.by(Sort.Direction.ASC, "name")
                ).stream()
                .map(this::toLowStockResponse)
                .toList();
    }

    private LowStockAlertResponse toLowStockResponse(Item item) {
        BigDecimal minLevel = normalize(item.getMinLevel());
        BigDecimal currentQty = normalize(item.getCurrentQty());
        BigDecimal shortage = minLevel.subtract(currentQty);
        return LowStockAlertResponse.builder()
                .itemId(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .category(item.getCategory())
                .currentQty(currentQty)
                .minLevel(minLevel)
                .shortage(shortage.max(BigDecimal.ZERO))
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
