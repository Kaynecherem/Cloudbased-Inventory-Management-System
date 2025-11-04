package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.ReferenceDataUpdateRequest;
import hr.algebra.cloudbased_inventory_management_system.entity.ReferenceDataType;
import hr.algebra.cloudbased_inventory_management_system.service.ReferenceDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import java.util.List;

@RestController
@RequestMapping("/api/reference")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReferenceDataController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/units")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public List<String> getUnits() {
        return referenceDataService.getValues(ReferenceDataType.UNIT);
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public List<String> getCategories() {
        return referenceDataService.getValues(ReferenceDataType.CATEGORY);
    }

    @GetMapping("/reason-codes")
    @PreAuthorize("hasAnyRole('MANAGER','STAFF')")
    public List<String> getReasonCodes() {
        return referenceDataService.getValues(ReferenceDataType.REASON_CODE);
    }

    @PutMapping("/{type}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<String>> updateReferenceData(
            @PathVariable String type,
            @Valid @RequestBody ReferenceDataUpdateRequest request
    ) {
        ReferenceDataType referenceType = parseType(type);
        return ResponseEntity.ok(referenceDataService.updateValues(referenceType, request));
    }

    private ReferenceDataType parseType(String rawType) {
        String normalized = rawType == null ? "" : rawType.trim().toLowerCase();
        return switch (normalized) {
            case "units", "unit" -> ReferenceDataType.UNIT;
            case "categories", "category" -> ReferenceDataType.CATEGORY;
            case "reason-codes", "reason", "reason_codes" -> ReferenceDataType.REASON_CODE;
            default -> throw new ResponseStatusException(BAD_REQUEST, "Unsupported reference data type: " + rawType);
        };
    }
}
