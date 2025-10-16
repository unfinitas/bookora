package fi.unfinitas.bookora.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for JWT token operations.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey signingKey;

    /**
     * Initialize and validate JWT configuration on application startup.
     * Validates that the secret key meets minimum security requirements.
     *
     * @throws IllegalStateException if the secret key is too short
     */
    @PostConstruct
    public void init() {
        log.info("Initializing JWT configuration...");

        final byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        final int keyLengthBits = keyBytes.length * 8;

        if (keyBytes.length < 32) {
            log.error("JWT secret key validation failed:");
            log.error("  Current: {} bytes ({} bits)", keyBytes.length, keyLengthBits);
            log.error("  Required: 32 bytes (256 bits)");
            log.error("  Solution: openssl rand -base64 32");

            throw new IllegalStateException(
                String.format("JWT secret key too short: %d bytes (required: 32)", keyBytes.length)
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        log.info("JWT configuration validated successfully. Key strength: {} bits", keyLengthBits);
    }

    /**
     * Get the secret key for signing JWT tokens.
     */
    private SecretKey getSigningKey() {
        return this.signingKey;
    }

    /**
     * Extract username from JWT token.
     */
    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from JWT token.
     */
    public Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from JWT token.
     */
    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from JWT token.
     */
    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired.
     */
    private Boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate access token for user.
     */
    public String generateAccessToken(final UserDetails userDetails) {
        final Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    /**
     * Generate refresh token for user.
     */
    public String generateRefreshToken(final UserDetails userDetails) {
        final Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    /**
     * Create JWT token with claims and subject.
     */
    private String createToken(final Map<String, Object> claims, final String subject, final Long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate JWT token.
     */
    public Boolean validateToken(final String token, final UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Get access token expiration time in milliseconds.
     */
    public Long getAccessTokenExpiration() {
        return expiration;
    }
}
