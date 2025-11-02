package hr.algebra.cloudbased_inventory_management_system.controller;

import hr.algebra.cloudbased_inventory_management_system.dto.UserResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.User;
import hr.algebra.cloudbased_inventory_management_system.service.UserMapper;
import hr.algebra.cloudbased_inventory_management_system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @GetMapping("/me")
    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new UsernameNotFoundException("User not authenticated");
        }
        User user = userService.getByUsernameOrEmail(authentication.getName());
        return userMapper.toResponse(user);
    }
}
