package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    boolean existsByNumber(String number);

    @EntityGraph(attributePaths = {"supplier", "createdBy", "lines", "lines.item"})
    Optional<PurchaseOrder> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"supplier", "createdBy", "lines", "lines.item"})
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);
}
