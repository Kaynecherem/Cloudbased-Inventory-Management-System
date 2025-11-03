package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.MovementRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.MovementResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.StockMovement;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MovementService {

    private final ItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public Page<MovementResponse> findMovements(
            Long itemId,
            MovementType type,
            Instant from,
            Instant to,
            String reason,
            Pageable pageable
    ) {
        return stockMovementRepository.findAll(
                StockMovementSpecifications.filter(itemId, type, from, to, sanitize(reason)),
                pageable
        ).map(this::toResponse);
    }

    @Transactional
    public MovementResponse recordMovement(MovementRequest request) {
        Item item = itemRepository.findByIdAndIsActiveTrue(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        String itemUnit = validateUnit(request.getUnit(), item.getUnit());

        BigDecimal quantity = normalizeQuantity(request.getQty());
        if (quantity == null || BigDecimal.ZERO.compareTo(quantity) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        BigDecimal currentQty = item.getCurrentQty() == null ? BigDecimal.ZERO : item.getCurrentQty();
        BigDecimal resultingQty = calculateResultingQuantity(currentQty, quantity, request.getType());

        item.setCurrentQty(resultingQty);

        try {
            itemRepository.saveAndFlush(item);
        } catch (OptimisticLockingFailureException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory was updated concurrently. Please retry.", ex);
        }

        StockMovement movement = StockMovement.builder()
                .item(item)
                .type(request.getType())
                .quantity(quantity)
                .resultingQuantity(resultingQty)
                .unit(itemUnit)
                .reasonCode(sanitize(request.getReasonCode()))
                .note(sanitize(request.getNote()))
                .build();

        movement.setCreatedAt(Instant.now());
        StockMovement savedMovement = stockMovementRepository.save(movement);
        return toResponse(savedMovement);
    }

    private BigDecimal calculateResultingQuantity(BigDecimal currentQty, BigDecimal quantity, MovementType type) {
        BigDecimal normalizedCurrent = currentQty == null ? BigDecimal.ZERO : currentQty;
        BigDecimal normalizedQuantity = quantity == null ? BigDecimal.ZERO : quantity;
        if (type == MovementType.IN) {
            return normalizedCurrent.add(normalizedQuantity);
        }
        BigDecimal resulting = normalizedCurrent.subtract(normalizedQuantity);
        if (resulting.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for outbound movement");
        }
        return resulting;
    }

    private MovementResponse toResponse(StockMovement movement) {
        return MovementResponse.builder()
                .id(movement.getId())
                .itemId(movement.getItem().getId())
                .itemName(movement.getItem().getName())
                .type(movement.getType())
                .qty(movement.getQuantity())
                .resultingQty(movement.getResultingQuantity())
                .unit(movement.getUnit())
                .reasonCode(movement.getReasonCode())
                .note(movement.getNote())
                .createdAt(movement.getCreatedAt())
                .build();
    }

    private String validateUnit(String requestedUnit, String itemUnit) {
        String sanitizedUnit = sanitize(requestedUnit);
        String sanitizedItemUnit = sanitize(itemUnit);
        if (!StringUtils.hasText(sanitizedItemUnit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item unit is not configured");
        }
        if (!sanitizedItemUnit.equalsIgnoreCase(sanitizedUnit)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Movement unit must match item unit");
        }
        return sanitizedItemUnit;
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros();
    }

    private String sanitize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

