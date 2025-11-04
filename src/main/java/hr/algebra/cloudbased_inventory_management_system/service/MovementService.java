package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.MovementRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.MovementResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceDataType;
import hr.algebra.cloudbased_inventory_management_system.entity.StockMovement;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.ReferenceDataRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.StockMovementSpecifications;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class MovementService {

    private static final int MAX_OPTIMISTIC_LOCK_ATTEMPTS = 3;

    private final ItemRepository itemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final TransactionTemplate transactionTemplate;

    public MovementService(
            ItemRepository itemRepository,
            StockMovementRepository stockMovementRepository,
            ReferenceDataRepository referenceDataRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.itemRepository = itemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

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

    public MovementResponse recordMovement(MovementRequest request) {
        int attempts = 0;
        while (true) {
            try {
                MovementResponse response = transactionTemplate.execute(status -> doRecordMovement(request));
                if (response != null) {
                    return response;
                }
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to record movement");
            } catch (OptimisticLockingFailureException ex) {
                if (++attempts >= MAX_OPTIMISTIC_LOCK_ATTEMPTS) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Inventory was updated concurrently. Please retry.", ex);
                }
            }
        }
    }

    private MovementResponse doRecordMovement(MovementRequest request) {
        Item item = itemRepository.findByIdAndIsActiveTrue(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        String itemUnit = validateUnit(request.getUnit(), item.getUnit());

        BigDecimal quantity = normalizeQuantity(request.getQty());
        if (quantity == null || BigDecimal.ZERO.compareTo(quantity) >= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than zero");
        }

        BigDecimal currentQty = item.getCurrentQty() == null ? BigDecimal.ZERO : item.getCurrentQty();
        BigDecimal resultingQty = calculateResultingQuantity(currentQty, quantity, request.getType());

        String reasonCode = normalizeReasonCode(request.getReasonCode());
        String note = sanitize(request.getNote());
        String clientRequestId = sanitize(request.getClientRequestId());

        if (clientRequestId != null && stockMovementRepository.existsByClientRequestId(clientRequestId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Movement with the same client request id already exists");
        }

        item.setCurrentQty(resultingQty);
        itemRepository.saveAndFlush(item);

        String createdBy = resolveCurrentUsername();

        StockMovement movement = StockMovement.builder()
                .item(item)
                .type(request.getType())
                .quantity(quantity)
                .resultingQuantity(resultingQty)
                .unit(itemUnit)
                .reasonCode(reasonCode)
                .note(note)
                .clientRequestId(clientRequestId)
                .createdBy(createdBy)
                .build();

        try {
            StockMovement savedMovement = stockMovementRepository.saveAndFlush(movement);
            return toResponse(savedMovement);
        } catch (DataIntegrityViolationException ex) {
            if (clientRequestId != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Movement with the same client request id already exists", ex);
            }
            throw ex;
        }
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
                .clientRequestId(movement.getClientRequestId())
                .createdBy(movement.getCreatedBy())
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

    private String normalizeReasonCode(String value) {
        String sanitized = sanitize(value);
        if (!StringUtils.hasText(sanitized)) {
            return null;
        }
        boolean exists = referenceDataRepository.existsByTypeAndValueIgnoreCase(ReferenceDataType.REASON_CODE, sanitized);
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown reason code");
        }
        return sanitized;
    }

    private String resolveCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve current user");
        }
        String username = sanitize(authentication.getName());
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unable to resolve current user");
        }
        return username;
    }

    private String sanitize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

