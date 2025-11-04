package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.ItemActivityResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.ItemResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.PageResponse;
import hr.algebra.cloudbased_inventory_management_system.service.ItemService;
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
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ItemController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ItemService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public PageResponse<ItemResponse> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        return PageResponse.from(service.findItems(search, supplierId, category, lowStock, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public ItemResponse getItem(@PathVariable Long id) {
        return service.getItem(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ItemResponse> createItem(@Valid @RequestBody ItemRequest request) {
        ItemResponse response = service.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ItemResponse updateItem(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.updateItem(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        service.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/activity")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public PageResponse<ItemActivityResponse> getItemActivity(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        Pageable pageable = buildPageable(page, size);
        return PageResponse.from(service.getItemActivity(id, pageable));
    }

    private Pageable buildPageable(Integer page, Integer size) {
        int pageNumber = page != null && page >= 0 ? page : DEFAULT_PAGE;
        int pageSize = size != null && size > 0 ? Math.min(size, MAX_PAGE_SIZE) : DEFAULT_SIZE;
        return PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.ASC, "name"));
    }
}
