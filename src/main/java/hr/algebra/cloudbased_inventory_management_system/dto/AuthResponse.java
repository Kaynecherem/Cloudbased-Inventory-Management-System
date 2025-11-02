package hr.algebra.cloudbased_inventory_management_system.dto;

import hr.algebra.cloudbased_inventory_management_system.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Role role;
    private UserResponse user;
}
