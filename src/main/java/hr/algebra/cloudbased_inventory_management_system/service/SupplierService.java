package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.SupplierRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.SupplierResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import hr.algebra.cloudbased_inventory_management_system.repository.ItemRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.SupplierRepository;
import hr.algebra.cloudbased_inventory_management_system.repository.SupplierSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final SupplierMapper supplierMapper;
    private final AuditContext auditContext;

    @Transactional(readOnly = true)
    public Page<SupplierResponse> findSuppliers(String search, Pageable pageable) {
        return supplierRepository.findAll(SupplierSpecifications.filterSuppliers(search, Boolean.TRUE), pageable)
                .map(supplierMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long id) {
        Supplier supplier = supplierRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));
        return supplierMapper.toResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request) {
        Supplier supplier = supplierMapper.toEntity(request);
        if (supplier.getIsActive() == null) {
            supplier.setIsActive(Boolean.TRUE);
        }
        String auditor = auditContext.getCurrentAuditor();
        supplier.setCreatedBy(auditor);
        supplier.setUpdatedBy(auditor);
        Supplier saved = supplierRepository.save(supplier);
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request) {
        Supplier existing = supplierRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        supplierMapper.updateEntity(existing, request);
        existing.setUpdatedBy(auditContext.getCurrentAuditor());
        Supplier saved = supplierRepository.save(existing);
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public void deleteSupplier(Long id) {
        Supplier existing = supplierRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier not found"));

        if (itemRepository.existsByPrimarySupplierIdAndIsActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Supplier is referenced by active items");
        }

        existing.setIsActive(Boolean.FALSE);
        existing.setUpdatedBy(auditContext.getCurrentAuditor());
        supplierRepository.save(existing);
    }
}
