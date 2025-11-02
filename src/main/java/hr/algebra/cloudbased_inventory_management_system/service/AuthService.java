package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.dto.AuthResponse;
import hr.algebra.cloudbased_inventory_management_system.dto.LoginRequest;
import hr.algebra.cloudbased_inventory_management_system.dto.UserResponse;
import hr.algebra.cloudbased_inventory_management_system.entity.User;
import hr.algebra.cloudbased_inventory_management_system.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper userMapper;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserService userService,
                       UserRepository userRepository,
                       CustomUserDetailsService userDetailsService,
                       UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse authenticate(LoginRequest request) {
        User user = userService.getByUsernameOrEmail(request.getIdentifier());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        UserResponse userResponse = userMapper.toResponse(user);
        return new AuthResponse(token, user.getRole(), userResponse);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String token) {
        String username;
        try {
            username = jwtService.extractUsername(token);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid token");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(token, userDetails)) {
            throw new BadCredentialsException("Invalid token");
        }

        String refreshedToken = jwtService.generateToken(userDetails);
        return new AuthResponse(refreshedToken, user.getRole(), userMapper.toResponse(user));
    }
}
