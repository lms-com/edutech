package com.lms.apigateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    // Chuyen secret-key thanh SecretKey Object cua java
    public SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey); // Chuyen thanh byte[]
        return Keys.hmacShaKeyFor(keyBytes);  // Tao SecretKey tu keyBytes
    }

    // Lay Claim tu token
    public Claims extractAllClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Kiem tra Token
    public void validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(getSecretKey()).build().parseClaimsJws(token);
    }

    // Lay UserId tu Token
    public String getUserId(String token) {
        return extractAllClaims(token).get("userId").toString();
    }

    // Lay Role va Permission (Authorities)
    public List<String> getAuthorities(String token) {
        return extractAllClaims(token).get("authorities", List.class);
    }

    // Lay Device fingerprint
    public String getDeviceFingerPrint(String token) {
        return extractAllClaims(token).get("deviceFingerPrint").toString();
    }
}
