package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "po_number_sequences")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderNumberSequence {

    @Id
    @Column(name = "sequence_date", nullable = false, unique = true)
    private LocalDate sequenceDate;

    @Column(name = "next_value", nullable = false)
    private Long nextValue;

    public static PurchaseOrderNumberSequence initialize(LocalDate date) {
        PurchaseOrderNumberSequence sequence = new PurchaseOrderNumberSequence();
        sequence.setSequenceDate(date);
        sequence.setNextValue(1L);
        return sequence;
    }

    public long getAndIncrement() {
        long current = nextValue == null ? 1L : nextValue;
        nextValue = current + 1;
        return current;
    }
}
