package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.AuthResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.ForgotPasswordRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.LoginRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.RefreshTokenRequest;
import hr.algebra.cloudbased_inventory_management_system.service.AuthService;
import hr.algebra.cloudbased_inventory_management_system.service.UserService;
import hr.algebra.cloudbased_inventory_management_system.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getToken()));
    }

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        User user = userService.getByUsernameOrEmail(request.getIdentifier());
        String message = String.format("Password reset instructions sent for user %s", user.getEmail());
        return ResponseEntity.ok(Map.of("message", message));
    }
}
