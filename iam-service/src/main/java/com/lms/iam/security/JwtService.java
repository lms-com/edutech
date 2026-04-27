package com.lms.iam.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken (CustomUserDetails customUserDetails, String deviceFingerPrint) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Lay Authorities cua User va chuyen thanh String de cho vao token
        List<String> authorities = customUserDetails.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList();

        // Them userId va permission vao claim cua token
        extraClaims.put("userId", customUserDetails.getUser().getId());
        extraClaims.put("authorities", authorities);
        extraClaims.put("deviceFingerPrint", deviceFingerPrint);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(customUserDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey () {
        byte[] keyBytes = Decoders.BASE64URL.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }


    // Cac mehtod ben duoi dung cho parse du lieu tu token

    private Claims extractAllClaims (String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String userName (String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    public String getUserId (String token) {
        // Lay claims
        Claims claims = extractAllClaims(token);
        // Lay claim userId tu claims
        return claims.get("userId").toString();
    }

    public List<String> getAuthorities (String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("authorities", List.class);
    }
}
