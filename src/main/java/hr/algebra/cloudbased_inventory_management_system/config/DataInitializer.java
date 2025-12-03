package hr.algebra.cloudbased_inventory_management_system.config;

import hr.algebra.cloudbased_inventory_management_system.entity.Role;
import hr.algebra.cloudbased_inventory_management_system.entity.User;
import hr.algebra.cloudbased_inventory_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfNotExists("admin", "admin@example.com", "Admin", "User", Role.ADMIN);
        createUserIfNotExists("manager", "manager@example.com", "Manager", "User", Role.MANAGER);
        createUserIfNotExists("staff", "staff@example.com", "Staff", "User", Role.STAFF);
    }

    private void createUserIfNotExists(String username, String email, String firstName, String lastName, Role role) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .password(passwordEncoder.encode("password"))
                .build();
        userRepository.save(user);
    }
}
