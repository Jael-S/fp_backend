package com.flowpolicy.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

  private final Key key;
  private final long expirationMs;

  public JwtUtil(
      @Value("${flowpolicy.jwt.secret}") String secret,
      @Value("${flowpolicy.jwt.expiration:86400000}") long expirationMs
  ) {
    // Permite que el secreto sea texto normal o base64; si no es base64 válido, se usa el texto como bytes.
    Key parsedKey;
    try {
      parsedKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    } catch (IllegalArgumentException ex) {
      parsedKey = Keys.hmacShaKeyFor(secret.getBytes());
    }
    this.key = parsedKey;
    this.expirationMs = expirationMs;
  }

  public String generateToken(String subject, Map<String, Object> claims) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setSubject(subject)
        .addClaims(claims)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }
}

