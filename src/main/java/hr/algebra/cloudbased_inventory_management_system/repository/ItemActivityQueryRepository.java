package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.ItemActivityType;
import hr.algebra.cloudbased_inventory_management_system.entity.MovementType;
import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ItemActivityQueryRepository {

    private static final String SOURCE_MOVEMENT = "MOVEMENT";

    private static final String BASE_QUERY = """
            SELECT
                sm.id AS event_id,
                'MOVEMENT' AS source_type,
                sm.created_at AS created_at,
                sm.type AS movement_type,
                sm.quantity AS quantity,
                sm.resulting_quantity AS resulting_quantity,
                sm.unit AS movement_unit,
                sm.reason_code AS reason_code,
                sm.note AS note,
                sm.created_by AS created_by,
                sm.po_id AS purchase_order_id,
                sm.po_line_id AS purchase_order_line_id,
                po.number AS purchase_order_number,
                po.status AS purchase_order_status,
                pol.qty_ordered AS purchase_order_line_qty_ordered,
                pol.qty_received AS purchase_order_line_qty_received,
                pol.unit AS purchase_order_line_unit,
                NULL AS item_event_type,
                NULL AS item_event_description
            FROM stock_movements sm
            LEFT JOIN purchase_orders po ON po.id = sm.po_id
            LEFT JOIN purchase_order_lines pol ON pol.id = sm.po_line_id
            WHERE sm.item_id = :itemId
            UNION ALL
            SELECT
                ia.id AS event_id,
                'ITEM_EVENT' AS source_type,
                ia.created_at AS created_at,
                NULL AS movement_type,
                ia.quantity_change AS quantity,
                NULL AS resulting_quantity,
                NULL AS movement_unit,
                NULL AS reason_code,
                ia.description AS note,
                NULL AS created_by,
                NULL AS purchase_order_id,
                NULL AS purchase_order_line_id,
                NULL AS purchase_order_number,
                NULL AS purchase_order_status,
                NULL AS purchase_order_line_qty_ordered,
                NULL AS purchase_order_line_qty_received,
                NULL AS purchase_order_line_unit,
                ia.type AS item_event_type,
                ia.description AS item_event_description
            FROM item_activities ia
            WHERE ia.item_id = :itemId
            """;

    @PersistenceContext
    private EntityManager entityManager;

    public Page<ItemActivityRow> findItemActivity(Long itemId, Pageable pageable) {
        String orderedQuery = "SELECT * FROM (" + BASE_QUERY + ") activities ORDER BY created_at DESC";
        Query query = entityManager.createNativeQuery(orderedQuery);
        query.setParameter("itemId", itemId);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();
        List<ItemActivityRow> rows = new ArrayList<>(resultList.size());
        for (Object[] row : resultList) {
            rows.add(mapRow(row));
        }

        String countQuerySql = "SELECT COUNT(*) FROM (" + BASE_QUERY + ") count_query";
        Query countQuery = entityManager.createNativeQuery(countQuerySql);
        countQuery.setParameter("itemId", itemId);
        Number totalElements = (Number) countQuery.getSingleResult();

        return new PageImpl<>(rows, pageable, totalElements.longValue());
    }

    private ItemActivityRow mapRow(Object[] row) {
        Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
        String source = (String) row[1];
        Instant createdAt = row[2] != null ? ((Timestamp) row[2]).toInstant() : null;
        MovementType movementType = row[3] != null ? MovementType.valueOf(row[3].toString()) : null;
        BigDecimal quantity = (BigDecimal) row[4];
        BigDecimal resultingQuantity = (BigDecimal) row[5];
        String unit = (String) row[6];
        String reasonCode = (String) row[7];
        String note = (String) row[8];
        String createdBy = (String) row[9];
        Long purchaseOrderId = row[10] != null ? ((Number) row[10]).longValue() : null;
        Long purchaseOrderLineId = row[11] != null ? ((Number) row[11]).longValue() : null;
        String purchaseOrderNumber = (String) row[12];
        PurchaseOrderStatus purchaseOrderStatus = row[13] != null ? PurchaseOrderStatus.valueOf(row[13].toString()) : null;
        BigDecimal purchaseOrderLineQtyOrdered = (BigDecimal) row[14];
        BigDecimal purchaseOrderLineQtyReceived = (BigDecimal) row[15];
        String purchaseOrderLineUnit = (String) row[16];
        ItemActivityType itemEventType = row[17] != null ? ItemActivityType.valueOf(row[17].toString()) : null;
        String itemEventDescription = (String) row[18];

        ItemActivitySource sourceType = SOURCE_MOVEMENT.equals(source) ? ItemActivitySource.MOVEMENT : ItemActivitySource.ITEM_EVENT;

        return new ItemActivityRow(
                id,
                sourceType,
                movementType,
                quantity,
                resultingQuantity,
                unit,
                reasonCode,
                note,
                createdBy,
                purchaseOrderId,
                purchaseOrderLineId,
                purchaseOrderNumber,
                purchaseOrderStatus,
                purchaseOrderLineQtyOrdered,
                purchaseOrderLineQtyReceived,
                purchaseOrderLineUnit,
                itemEventType,
                itemEventDescription,
                createdAt
        );
    }

    public enum ItemActivitySource {
        MOVEMENT,
        ITEM_EVENT
    }

    public record ItemActivityRow(
            Long id,
            ItemActivitySource source,
            MovementType movementType,
            BigDecimal quantity,
            BigDecimal resultingQuantity,
            String unit,
            String reasonCode,
            String note,
            String createdBy,
            Long purchaseOrderId,
            Long purchaseOrderLineId,
            String purchaseOrderNumber,
            PurchaseOrderStatus purchaseOrderStatus,
            BigDecimal purchaseOrderLineQtyOrdered,
            BigDecimal purchaseOrderLineQtyReceived,
            String purchaseOrderLineUnit,
            ItemActivityType itemEventType,
            String itemEventDescription,
            Instant createdAt
    ) {
    }
}
