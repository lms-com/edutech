package com.lms.finance.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component

public class VnPayConfig {

    public static String hashAllFields (Map<String, String> fields, String secretKey) {
        // Lay danh sach fieldName
        List<String> fieldNames = new ArrayList(fields.keySet());
        // Sap xep theo thu tu chu cai dau
        Collections.sort(fieldNames);
        // tien hanh hash
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = fieldNames.iterator();
        while (it.hasNext()) {
            String fieldName = it.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                sb.append(fieldName);
                sb.append('=');
                sb.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
            if (it.hasNext()) {
                sb.append('&');
            }
        }
        return hmacSha512(secretKey, sb.toString());
    }


    public static String hmacSha512 (final String key, final String data) {
        try {
            if (key == null || data == null) {
                throw new NullPointerException("key or data is null");
            }
            // Tao doi tuong ma hoa
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            // chuyen key thanh chuoi byte
            byte[] keyBytes = key.getBytes();
            // tao doi tuong SecretKeySpec de lam param cho doi tuong ma hoa
            final SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA512");
            // truyen SecretKeySpec vao ham init cua doi tuong ma hoa
            hmac512.init(secretKeySpec);
            // chuyen data thanh chuoi byte
            byte[] dataBytes = data.getBytes();
            // Sau do truyen vao ham doFinal cua cong cu ma hoa de lay data sau khi ma hoa
            byte[] result = hmac512.doFinal(dataBytes);
            // Tien hanh chuyen ket qua tren sang he 16:
                // tao StringBuffer voi length gap doi
            StringBuffer sb = new StringBuffer(result.length * 2);
                // chuyen moi byte cua data da ma hoa sang he thap luc phan roi noi vao StringBuffer
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String getIpAddress (HttpServletRequest req) {
        String ip = req.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return req.getRemoteAddr();
        }
        return ip;
    }
}
