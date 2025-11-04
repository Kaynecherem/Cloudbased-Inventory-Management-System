package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.controller.support.PageableUtil;
import hr.algebra.cloudbased_inventory_management_system.dto.MovementRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.MovementResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.PageResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.service.MovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Set;

@RestController
@RequestMapping("/api/movements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MovementController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final MovementService movementService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public PageResponse<MovementResponse> getMovements(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort
    ) {
        Pageable pageable = PageableUtil.buildPageable(
                page,
                size,
                sort,
                DEFAULT_PAGE,
                DEFAULT_SIZE,
                MAX_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createdAt"),
                Set.of("createdAt", "quantity", "resultingQuantity", "type", "reasonCode")
        );
        return PageResponse.from(movementService.findMovements(itemId, type, from, to, reason, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public ResponseEntity<MovementResponse> createMovement(@Valid @RequestBody MovementRequest request) {
        MovementResponse response = movementService.recordMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}

