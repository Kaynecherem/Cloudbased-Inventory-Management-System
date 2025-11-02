package hr.algebra.cloudbased_inventory_management_system.config;

import hr.algebra.cloudbased_inventory_management_system.entity.Role;
import hr.algebra.cloudbased_inventory_management_system.entity.User;
import hr.algebra.cloudbased_inventory_management_system.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfNotExists("manager", "manager@example.com", "Manager", "User", Role.MANAGER);
        createUserIfNotExists("staff", "staff@example.com", "Staff", "User", Role.STAFF);
    }

    private void createUserIfNotExists(String username, String email, String firstName, String lastName, Role role) {
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPassword(passwordEncoder.encode("password"));
        userRepository.save(user);
    }
}
