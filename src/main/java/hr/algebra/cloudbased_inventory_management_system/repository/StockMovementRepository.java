package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.StockMovement;
import hr.algebra.cloudbased_inventory_management_system.dto.TopMoverItem;
import hr.algebra.cloudbased_inventory_management_system.dto.UsageReportItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    boolean existsByClientRequestId(String clientRequestId);

    @Query("""
            SELECT new hr.algebra.cloudbased_inventory_management_system.dto.UsageReportItem(
                sm.item.id,
                sm.item.sku,
                sm.item.name,
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.IN THEN sm.quantity ELSE 0 END),
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END),
                :windowDays
            )
            FROM StockMovement sm
            WHERE sm.item.isActive = TRUE
              AND (:from IS NULL OR sm.createdAt >= :from)
              AND (:to IS NULL OR sm.createdAt <= :to)
            GROUP BY sm.item.id, sm.item.sku, sm.item.name
            HAVING SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END) > 0
            ORDER BY SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END) DESC
            """)
    List<UsageReportItem> findUsageReport(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("windowDays") BigDecimal windowDays,
            Pageable pageable
    );

    @Query("""
            SELECT new hr.algebra.cloudbased_inventory_management_system.dto.TopMoverItem(
                sm.item.id,
                sm.item.sku,
                sm.item.name,
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.IN THEN sm.quantity ELSE 0 END),
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END)
            )
            FROM StockMovement sm
            WHERE sm.item.isActive = TRUE
              AND (:from IS NULL OR sm.createdAt >= :from)
              AND (:to IS NULL OR sm.createdAt <= :to)
            GROUP BY sm.item.id, sm.item.sku, sm.item.name
            ORDER BY
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.IN THEN sm.quantity ELSE 0 END)
              + SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END) DESC
            """)
    List<TopMoverItem> findTopMoversByMovement(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("""
            SELECT new hr.algebra.cloudbased_inventory_management_system.dto.TopMoverItem(
                sm.item.id,
                sm.item.sku,
                sm.item.name,
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.IN THEN sm.quantity ELSE 0 END),
                SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END)
            )
            FROM StockMovement sm
            WHERE sm.item.isActive = TRUE
              AND (:from IS NULL OR sm.createdAt >= :from)
              AND (:to IS NULL OR sm.createdAt <= :to)
            GROUP BY sm.item.id, sm.item.sku, sm.item.name
            ORDER BY SUM(CASE WHEN sm.type = hr.algebra.cloudbased_inventory_management_system.entity.MovementType.OUT THEN sm.quantity ELSE 0 END) DESC
            """)
    List<TopMoverItem> findTopMoversByOut(
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    @Query("SELECT DISTINCT TRIM(sm.reasonCode) FROM StockMovement sm WHERE sm.reasonCode IS NOT NULL AND sm.reasonCode <> ''")
    List<String> findDistinctReasonCodes();

    boolean existsByReasonCodeIgnoreCase(String reasonCode);
}

