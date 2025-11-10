package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "suppliers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_suppliers_name", columnNames = "name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_name")
    private String contactName;

    @Column
    private String email;

    @Column
    private String phone;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(name = "created_by", nullable = false, length = 150, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 150)
    private String updatedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (isActive == null) {
            isActive = Boolean.TRUE;
        }
        if (createdBy == null) {
            createdBy = "system";
        }
        if (updatedBy == null) {
            updatedBy = createdBy;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
        if (updatedBy == null) {
            updatedBy = createdBy != null ? createdBy : "system";
        }
    }
}
