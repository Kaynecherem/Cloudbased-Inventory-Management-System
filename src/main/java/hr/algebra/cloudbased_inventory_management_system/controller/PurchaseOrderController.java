package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.PageResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderReceiveRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.PurchaseOrderResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import hr.algebra.cloudbased_inventory_management_system.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/pos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public PageResponse<PurchaseOrderResponse> getPurchaseOrders(
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        return purchaseOrderService.findPurchaseOrders(status, supplierId, from, to, pageable);
    }

    @GetMapping("/{id}")
    public PurchaseOrderResponse getPurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.getPurchaseOrder(id);
    }

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public PurchaseOrderResponse updatePurchaseOrder(@PathVariable Long id, @Valid @RequestBody PurchaseOrderRequest request) {
        return purchaseOrderService.updatePurchaseOrder(id, request);
    }

    @PostMapping("/{id}/submit")
    public PurchaseOrderResponse submitPurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.submitPurchaseOrder(id);
    }

    @PostMapping("/{id}/receive")
    public PurchaseOrderResponse receivePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody List<PurchaseOrderReceiveRequest> requests
    ) {
        return purchaseOrderService.receivePurchaseOrder(id, requests);
    }

    @PostMapping("/{id}/cancel")
    public PurchaseOrderResponse cancelPurchaseOrder(@PathVariable Long id) {
        return purchaseOrderService.cancelPurchaseOrder(id);
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNumber = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_SIZE;
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
