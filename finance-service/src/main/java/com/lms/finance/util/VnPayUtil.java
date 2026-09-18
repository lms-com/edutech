package com.lms.finance.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component

public class VnPayUtil {

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
        return hmacSHA512(secretKey, sb.toString());
    }


    public static String hmacSHA512 (final String key, final String data) {
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

    public static void main (String[] args) {
        String tmnCode = "2L11EZ3R";
        String hashSecret = "GLRQMIUPFAFOCVECZWVQYCBORJRXOKWE";
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String orderType = "other";
        String bankCode = "NCB";
        long amount = 2288209 * 100;
        String currency = "VND";
        String orderId = "e57831be-cdd7-461b-a4c2-fe2eba8a3de5";
        String paymentRef = "e57831be-cdd7-461b-a4c2-fe2eba8a3de5";

        String vnp_IpAddr = "127.0.0.1";

        String vnp_TmnCode = tmnCode;

        Map<String, String> vnp_Params = new HashMap<>();
        //vnp_Params.put("vnp_Version", vnp_Version);
        //vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_TransactionNo", "14123456");
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        //vnp_Params.put("vnp_CurrCode", currency);
        vnp_Params.put("vnp_PayDate", "20260817171500");
        vnp_Params.put("vnp_ResponseCode", "00"); // Mã 00 báo thanh toán thành công xịn
        vnp_Params.put("vnp_TransactionStatus", "00");
        vnp_Params.put("vnp_CardType", "ATM");
        vnp_Params.put("vnp_BankTranNo","VNP14071302");


        if (bankCode != null && !bankCode.isEmpty()) {
            vnp_Params.put("vnp_BankCode", bankCode);
        }
        vnp_Params.put("vnp_TxnRef", paymentRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh-toan-don-hang-" + orderId);
        //vnp_Params.put("vnp_OrderType", orderType);

        String locate = "vn";
        if (locate != null && !locate.isEmpty()) {
            vnp_Params.put("vnp_Locale", locate);
        } else {
            vnp_Params.put("vnp_Locale", "vn");
        }
        vnp_Params.put("vnp_ReturnUrl", "http://localhost:8080/finance-service/api/v1/payments/vnpay-callback");
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        //vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        //vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List fieldNames = new ArrayList(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = (String) itr.next();
            String fieldValue = (String) vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                //Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }
        String vnp_SecureHash = VnPayUtil.hmacSHA512(hashSecret, hashData.toString());
        vnp_Params.put("vnp_SecureHash", vnp_SecureHash);
        System.out.println("\033[43;32m" +  vnp_SecureHash + "\n\033[0m");
    }
}
