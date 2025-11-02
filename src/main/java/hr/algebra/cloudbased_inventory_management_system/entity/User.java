package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;

    private String lastName;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        if (username == null || username.isBlank()) {
            username = generateUsername();
        }
    }

    private String generateUsername() {
        var base = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            base.append(firstName.strip().toLowerCase(Locale.ROOT));
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!base.isEmpty()) {
                base.append('.');
            }
            base.append(lastName.strip().toLowerCase(Locale.ROOT));
        }
        if (base.isEmpty()) {
            base.append("user");
        }
        base.append('-').append(UUID.randomUUID().toString().substring(0, 8));
        return base.toString();
    }
}
