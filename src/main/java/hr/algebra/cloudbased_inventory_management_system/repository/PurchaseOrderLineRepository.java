package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderLine;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(pol) > 0 THEN TRUE ELSE FALSE END
            FROM PurchaseOrderLine pol
            WHERE pol.item.id = :itemId
              AND pol.purchaseOrder.status IN :statuses
            """)
    boolean existsOpenLinesForItem(@Param("itemId") Long itemId, @Param("statuses") Collection<PurchaseOrderStatus> statuses);
}
