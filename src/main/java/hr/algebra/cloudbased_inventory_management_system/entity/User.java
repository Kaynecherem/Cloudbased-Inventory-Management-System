package hr.algebra.cloudbased_inventory_management_system.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

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
            if (base.length() > 0) {
                base.append('.');
            }
            base.append(lastName.strip().toLowerCase(Locale.ROOT));
        }
        if (base.length() == 0) {
            base.append("user");
        }
        base.append('-').append(UUID.randomUUID().toString().substring(0, 8));
        return base.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
