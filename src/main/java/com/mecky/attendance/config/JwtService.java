package com.mecky.attendance.config;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
@Service public class JwtService {
 private final SecretKey key; private final long minutes;
 public JwtService(@Value("${app.jwt.secret}") String secret,@Value("${app.jwt.minutes}") long minutes){
  if(secret.getBytes(StandardCharsets.UTF_8).length<32)throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
  key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));this.minutes=minutes;
 }
 public String issue(String username,String role){Instant now=Instant.now();return Jwts.builder().subject(username).claim("role",role).issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(minutes*60))).signWith(key).compact();}
 public Claims parse(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();}
}
