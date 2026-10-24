package mz.mobile.bussiness.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import mz.mobile.bussiness.security.AuthUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;


//@Service
//public class JwtService {
//
//    @Value("${app.jwt.secret}")
//    private String secret;
//
//    @Value("${app.jwt.expiration-ms}")
//    private long expirationMs;
//
//    private SecretKey getSigningKey() {
//        byte[] keyBytes = Decoders.BASE64.decode(secret);
//        return Keys.hmacShaKeyFor(keyBytes);
//    }
//
//    public String generateToken(UserDetails userDetails) {
//        Date now = new Date();
//        Date expiry = new Date(
//                now.getTime() + expirationMs);
//
//        return Jwts.builder()
//                .subject(userDetails.getUsername())
//                .issuedAt(now)
//                .expiration(expiry)
//                .signWith(getSigningKey())
//                .compact();
//    }
//
//    public String extractUsername(String token) {
//        return extractAllClaims(token).getSubject();
//    }
//
//    public boolean isTokenValid(
//            String token,
//            UserDetails userDetails) {
//
//        String username = extractUsername(token);
//
//        return username.equals(userDetails.getUsername())
//                && extractAllClaims(token)
//                .getExpiration()
//                .after(new Date());
//    }
//
//    private Claims extractAllClaims(String token) {
//        return Jwts.parser()
//                .verifyWith(getSigningKey())
//                .build()
//                .parseSignedClaims(token)
//                .getPayload();
//    }
//
//    public long getExpirationMs() {
//        return expirationMs;
//    }
//}

@Service
public class JwtService {

    private static final String ISSUER = "second-hand-mobile";

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret is missing. Check your .env file.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("app.jwt.secret must be Base64-encoded.", e);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must decode to at least 32 bytes (256 bits). "
                            + "Generate one with: openssl rand - base64 48");
        }

        if (expirationMs <= 0) {
            throw new IllegalStateException("app.jwt.expiration-ms must be positive.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

//    public String generateToken(UserDetails userDetails) {
//        Date now = new Date();
//        return Jwts.builder()
//                .issuer(ISSUER)
//                .subject(userDetails.getUsername())
//                .issuedAt(now)
//                .expiration(new Date(now.getTime() + expirationMs))
//                .signWith(signingKey)
//                .compact();
//    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

//    public boolean isTokenValid(String token, UserDetails userDetails) {
//        Claims claims = extractAllClaims(token);   // already verifies signature, issuer and expiry
//        return userDetails.getUsername().equals(claims.getSubject())
//                && claims.getExpiration().after(new Date());
//    }

    public String generateToken(AuthUser user) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim("tv", user.getTokenVersion())          // token version
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token, AuthUser user) {
        Claims c = extractAllClaims(token);
        Integer tv = c.get("tv", Integer.class);
        return user.getUsername().equals(c.getSubject())
                && tv != null && tv == user.getTokenVersion()   // revoked or old tokens fail here
                && c.getExpiration().after(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}