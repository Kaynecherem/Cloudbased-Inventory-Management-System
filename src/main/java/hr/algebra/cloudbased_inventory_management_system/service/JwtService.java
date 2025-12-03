package hr.algebra.cloudbased_inventory_management_system.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    private static final String DEFAULT_SECRET = "5d828f9028f04b48a865e0e1f1283d1f5d828f9028f04b48a865e0e1f1283d1f";

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtService(@Value("${security.jwt.secret:${JWT_SECRET:}}") String secret,
                      @Value("${security.jwt.expiration-ms:3600000}") long jwtExpirationMs) {
        String resolvedSecret = secret;
        if (resolvedSecret == null || resolvedSecret.isBlank()) {
            resolvedSecret = DEFAULT_SECRET;
            logger.warn("Missing JWT secret configuration. Using default secret; please configure 'security.jwt.secret' or 'JWT_SECRET' in production.");
        }
        this.secretKey = Keys.hmacShaKeyFor(resolvedSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth != null && auth.startsWith("ROLE_"))
                .findFirst()
                .map(auth -> auth.substring("ROLE_".length()))
                .orElse(null);

        return role != null
                ? generateToken(userDetails, Map.of("role", role))
                : generateToken(userDetails, Map.of());
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(jwtExpirationMs)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> {
            Object role = claims.get("role");
            return role != null ? role.toString() : null;
        });
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
