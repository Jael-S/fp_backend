package com.flowpolicy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

  private static final String CLAIM_ROL = "rol";
  private static final String CLAIM_EMPRESA_ID = "empresaId";

  private final Key key;
  private final long expirationMs;

  public JwtUtil(
      @Value("${flowpolicy.jwt.secret}") String secret,
      @Value("${flowpolicy.jwt.expiration}") long expirationMs
  ) {
    this.key = parseSecret(secret);
    this.expirationMs = expirationMs;
  }

  public String generateToken(UserDetails userDetails, String rol, String empresaId) {
    Map<String, Object> claims = new HashMap<>();
    claims.put(CLAIM_ROL, rol);
    claims.put(CLAIM_EMPRESA_ID, empresaId);

    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);

    return Jwts.builder()
        .setSubject(userDetails.getUsername())
        .addClaims(claims)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public boolean validateToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception ex) {
      return false;
    }
  }

  public String extractEmail(String token) {
    return parseClaims(token).getSubject();
  }

  public String extractRol(String token) {
    Object value = parseClaims(token).get(CLAIM_ROL);
    return value == null ? null : value.toString();
  }

  public String extractEmpresaId(String token) {
    Object value = parseClaims(token).get(CLAIM_EMPRESA_ID);
    return value == null ? null : value.toString();
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private static Key parseSecret(String secret) {
    try {
      return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    } catch (IllegalArgumentException ex) {
      return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
  }
}

