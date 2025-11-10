package hr.algebra.cloudbased_inventory_management_system.service;

import hr.algebra.cloudbased_inventory_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuditContext {

    private static final String SYSTEM_USER = "system";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public String getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return SYSTEM_USER;
        }

        String identifier = authentication.getName();
        if (StringUtils.hasText(identifier)) {
            identifier = identifier.trim();
        }
        if (!StringUtils.hasText(identifier)) {
            return SYSTEM_USER;
        }

        return userRepository.findByUsernameOrEmail(identifier, identifier)
                .map(user -> {
                    if (StringUtils.hasText(user.getEmail())) {
                        return user.getEmail();
                    }
                    if (StringUtils.hasText(user.getUsername())) {
                        return user.getUsername();
                    }
                    return SYSTEM_USER;
                })
                .orElse(identifier);
    }
}

