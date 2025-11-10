package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.MovementRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PageResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderLineRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderLineResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderReceiveRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Item;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrder;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderLine;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderNumberSequence;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.PurchaseOrderNumberSequenceRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.PurchaseOrderRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.PurchaseOrderSpecifications;
import hr.algebra.cloudbased_inventory_management_system.repository.SupplierRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private static final String RECEIPT_REASON_CODE = "PO_RECEIVE";
    private static final int MAX_NOTE_LENGTH = 500;
    private static final DateTimeFormatter ORDER_NUMBER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd").withLocale(Locale.ROOT);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderNumberSequenceRepository purchaseOrderNumberSequenceRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final MovementService movementService;
    private final AuditContext auditContext;

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> findPurchaseOrders(
            PurchaseOrderStatus status,
            Long supplierId,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        Page<PurchaseOrder> page = purchaseOrderRepository.findAll(
                PurchaseOrderSpecifications.filter(status, supplierId, from, to),
                pageable
        );
        return PageResponse.from(page.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));
        return toResponse(order);
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(@Valid PurchaseOrderRequest request) {
        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        String auditor = auditContext.getCurrentAuditor();

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplier(supplier);
        order.setCreatedBy(auditor);
        order.setUpdatedBy(auditor);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        order.setEta(resolveEta(request.getEta(), supplier));
        order.setNumber(generateOrderNumber());

        mapLines(order, request.getLines(), auditor);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrder(Long id, @Valid PurchaseOrderRequest request) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft purchase orders can be edited");
        }

        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        order.setSupplier(supplier);
        order.setEta(resolveEta(request.getEta(), supplier));
        order.setUpdatedBy(auditContext.getCurrentAuditor());

        order.clearLines();
        mapLines(order, request.getLines(), order.getUpdatedBy());

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse submitPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only draft purchase orders can be submitted");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order must contain at least one line");
        }

        order.setStatus(PurchaseOrderStatus.PENDING);
        order.setUpdatedBy(auditContext.getCurrentAuditor());
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id, List<@Valid PurchaseOrderReceiveRequest> receiveRequests) {
        if (receiveRequests == null || receiveRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one line is required for receiving");
        }

        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.PENDING && order.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending purchase orders can be received");
        }

        Map<Long, BigDecimal> quantities = aggregateQuantities(receiveRequests);
        String auditor = auditContext.getCurrentAuditor();

        Map<Long, PurchaseOrderLine> lineIndex = order.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, line -> line));

        for (Map.Entry<Long, BigDecimal> entry : quantities.entrySet()) {
            PurchaseOrderLine line = lineIndex.get(entry.getKey());
            if (line == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line does not belong to this purchase order");
            }
            BigDecimal receivedQty = normalize(entry.getValue());
            if (receivedQty == null || receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received quantity must be greater than zero");
            }

            BigDecimal newTotal = line.getQtyReceived().add(receivedQty);
            if (newTotal.compareTo(line.getQtyOrdered()) > 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received quantity exceeds ordered quantity");
            }
            line.setQtyReceived(newTotal);
            line.setUpdatedBy(auditor);

            MovementRequest movementRequest = MovementRequest.builder()
                    .itemId(line.getItem().getId())
                    .type(MovementType.IN)
                    .qty(receivedQty)
                    .unit(line.getUnit())
                    .reasonCode(RECEIPT_REASON_CODE)
                    .note(buildReceiptNote(order, line))
                    .purchaseOrderId(order.getId())
                    .purchaseOrderLineId(line.getId())
                    .build();
            movementService.recordMovement(movementRequest);
        }

        boolean allReceived = order.getLines().stream()
                .allMatch(line -> line.getQtyReceived().compareTo(line.getQtyOrdered()) >= 0);
        order.setStatus(allReceived ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        order.setUpdatedBy(auditor);

        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toResponse(saved);
    }

    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long id) {
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase order not found"));

        if (order.getStatus() != PurchaseOrderStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending purchase orders can be cancelled");
        }

        List<PurchaseOrderLine> lines = order.getLines();
        boolean hasReceivedQuantities = lines != null && lines.stream()
                .anyMatch(line -> line.getQtyReceived() != null && line.getQtyReceived().compareTo(BigDecimal.ZERO) > 0);
        if (hasReceivedQuantities) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a purchase order with received quantities");
        }

        order.setStatus(PurchaseOrderStatus.CANCELLED);
        order.setUpdatedBy(auditContext.getCurrentAuditor());
        PurchaseOrder saved = purchaseOrderRepository.save(order);
        return toResponse(saved);
    }

    private Instant resolveEta(Instant requestedEta, Supplier supplier) {
        if (requestedEta != null) {
            return requestedEta;
        }
        Integer leadTimeDays = supplier.getLeadTimeDays();
        if (leadTimeDays == null || leadTimeDays <= 0) {
            return null;
        }
        return Instant.now().plus(leadTimeDays, ChronoUnit.DAYS);
    }

    private void mapLines(PurchaseOrder order, List<PurchaseOrderLineRequest> lineRequests, String auditor) {
        if (lineRequests == null || lineRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purchase order requires at least one line");
        }

        Set<Long> seenItems = new HashSet<>();

        for (PurchaseOrderLineRequest lineRequest : lineRequests) {
            if (lineRequest == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line details are required");
            }
            if (lineRequest.getItemId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item identifier is required");
            }
            if (!seenItems.add(lineRequest.getItemId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate items are not allowed in a purchase order");
            }
            Item item = itemRepository.findByIdAndIsActiveTrue(lineRequest.getItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

            String sanitizedUnit = sanitize(lineRequest.getUnit());
            if (sanitizedUnit == null || !sanitizedUnit.equalsIgnoreCase(item.getUnit())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line unit must match item unit");
            }

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setItem(item);
            line.setQtyOrdered(normalize(lineRequest.getQtyOrdered()));
            if (line.getQtyOrdered() == null || line.getQtyOrdered().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ordered quantity must be greater than zero");
            }
            line.setQtyReceived(BigDecimal.ZERO);
            line.setUnit(item.getUnit());
            BigDecimal price = normalize(lineRequest.getPrice());
            if (price != null && price.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price cannot be negative");
            }
            line.setPrice(price);
            line.setCreatedBy(auditor);
            line.setUpdatedBy(auditor);

            order.addLine(line);
        }
    }

    private Map<Long, BigDecimal> aggregateQuantities(List<PurchaseOrderReceiveRequest> receiveRequests) {
        Map<Long, BigDecimal> aggregated = new HashMap<>();
        for (PurchaseOrderReceiveRequest request : receiveRequests) {
            Long lineId = request.getLineId();
            if (lineId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Line identifier is required");
            }
            BigDecimal quantity = normalize(request.getReceivedQty());
            if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Received quantity must be greater than zero");
            }
            aggregated.merge(lineId, quantity, BigDecimal::add);
        }
        return aggregated;
    }

    private String buildReceiptNote(PurchaseOrder order, PurchaseOrderLine line) {
        String base = "PO " + order.getNumber() + " - Line " + line.getId();
        if (base.length() > MAX_NOTE_LENGTH) {
            return base.substring(0, MAX_NOTE_LENGTH);
        }
        return base;
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderLineResponse> lineResponses = new ArrayList<>();
        for (PurchaseOrderLine line : order.getLines()) {
            lineResponses.add(PurchaseOrderLineResponse.builder()
                    .id(line.getId())
                    .itemId(line.getItem().getId())
                    .itemName(line.getItem().getName())
                    .qtyOrdered(line.getQtyOrdered())
                    .qtyReceived(line.getQtyReceived())
                    .unit(line.getUnit())
                    .price(line.getPrice())
                    .createdBy(line.getCreatedBy())
                    .updatedBy(line.getUpdatedBy())
                    .build());
        }

        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .number(order.getNumber())
                .supplierId(order.getSupplier().getId())
                .supplierName(order.getSupplier().getName())
                .status(order.getStatus())
                .eta(order.getEta())
                .createdBy(order.getCreatedBy())
                .updatedBy(order.getUpdatedBy())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .lines(lineResponses)
                .build();
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros();
    }

    private String sanitize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String generateOrderNumber() {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zoneId);

        PurchaseOrderNumberSequence sequence = purchaseOrderNumberSequenceRepository
                .findBySequenceDateForUpdate(today)
                .orElseGet(() -> PurchaseOrderNumberSequence.initialize(today));

        long sequenceValue = sequence.getAndIncrement();
        purchaseOrderNumberSequenceRepository.saveAndFlush(sequence);

        String datePart = ORDER_NUMBER_DATE_FORMATTER.format(today);
        return String.format(Locale.ROOT, "PO-%s-%04d", datePart, sequenceValue);
    }
}
