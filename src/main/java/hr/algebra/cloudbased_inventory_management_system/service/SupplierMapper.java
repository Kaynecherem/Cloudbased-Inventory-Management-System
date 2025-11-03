package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.SupplierRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.SupplierResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SupplierMapper {

    public Supplier toEntity(SupplierRequest request) {
        Supplier supplier = new Supplier();
        updateEntity(supplier, request);
        return supplier;
    }

    public void updateEntity(Supplier supplier, SupplierRequest request) {
        supplier.setName(trimOrNull(request.getName()));
        supplier.setContactName(trimOrNull(request.getContactName()));
        supplier.setEmail(normalizeEmail(request.getEmail()));
        supplier.setPhone(trimOrNull(request.getPhone()));
        supplier.setLeadTimeDays(request.getLeadTimeDays());
        supplier.setAddress(trimOrNull(request.getAddress()));
        supplier.setNotes(trimOrNull(request.getNotes()));
        if (request.getIsActive() != null) {
            supplier.setIsActive(request.getIsActive());
        }
    }

    public SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactName(supplier.getContactName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .leadTimeDays(supplier.getLeadTimeDays())
                .address(supplier.getAddress())
                .notes(supplier.getNotes())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }
}
