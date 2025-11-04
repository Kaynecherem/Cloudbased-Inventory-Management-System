package hr.algebra.cloudbased_inventory_management_system.repository;

import hr.algebra.cloudbased_inventory_management_system.entity.PurchaseOrderNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface PurchaseOrderNumberSequenceRepository extends JpaRepository<PurchaseOrderNumberSequence, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select seq from PurchaseOrderNumberSequence seq where seq.sequenceDate = :sequenceDate")
    Optional<PurchaseOrderNumberSequence> findBySequenceDateForUpdate(@Param("sequenceDate") LocalDate sequenceDate);
}
