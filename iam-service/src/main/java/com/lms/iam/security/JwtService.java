package com.lms.iam.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken (CustomUserDetails customUserDetails, String deviceFingerPrint) {
        Map<String, Object> extraClaims = new HashMap<>();
        // Them userId va permission vao claim cua token
        extraClaims.put("userId", customUserDetails.getUser().getId());
        extraClaims.put("permissions", customUserDetails.getPermissions());
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
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
