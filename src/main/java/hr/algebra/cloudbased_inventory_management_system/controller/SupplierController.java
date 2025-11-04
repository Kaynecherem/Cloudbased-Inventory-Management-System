package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.PageResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.SupplierRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.SupplierResponse;
import hr.algebra.cloudbased_inventory_management_system.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SupplierController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public PageResponse<SupplierResponse> getSuppliers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        return PageResponse.from(supplierService.findSuppliers(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public SupplierResponse getSupplier(@PathVariable Long id) {
        return supplierService.getSupplier(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public SupplierResponse updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.updateSupplier(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNumber = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_SIZE;
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "name"));
    }
}
