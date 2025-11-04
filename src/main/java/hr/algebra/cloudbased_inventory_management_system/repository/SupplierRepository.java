package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    Optional<Supplier> findByIdAndIsActiveTrue(Long id);

    List<Supplier> findByIdInAndIsActiveTrue(Collection<Long> ids);
}
