package com.lms.finance.service.impl;

import com.lms.common.exception.AppException;
import com.lms.finance.config.VnPayConfig;
import com.lms.finance.dto.message.PaymentProcessMessage;
import com.lms.finance.dto.request.CreatePaymentRequest;
import com.lms.finance.entity.Payment;
import com.lms.finance.enums.PaymentMethod;
import com.lms.finance.enums.PaymentStatus;
import com.lms.finance.exception.FinanceErrorCode;
import com.lms.finance.repository.PaymentRepository;
import com.lms.finance.service.PaymentService;
import com.lms.finance.service.PaymentStrategy;
import com.lms.finance.util.VnPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.lms.finance.config.RabbitMQConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final VnPayConfig vnPayConfig;
    private final PaymentRepository paymentRepository;
    private final Map<String, PaymentStrategy> paymentMap;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public String createPayment (HttpServletRequest httpReq, CreatePaymentRequest createReq) {
        log.info("🚀 Finance Service:createPayment for orderId {}- start", createReq.getOrderId());
        PaymentStrategy paymentStrategy = paymentMap.get(createReq.getPaymentMethod());
        if (paymentStrategy == null) {
            throw new AppException(
                    FinanceErrorCode.UNSUPPORTED_PAYMENT_METHOD,
                    String.format("Payment method %s not supported", createReq.getPaymentMethod())
            );
        }
        String orderId = createReq.getOrderId();
        int attempts = countAllowedRetryPayment(orderId);
        if (attempts < 0) {
            throw new AppException(
                    FinanceErrorCode.INVALID_PAYMENT,
                    "Processed order cannot be retried to pay"
            );
        }
        String paymentRef = orderId + "_" + (attempts + 1);
        createReq.setPaymentRef(paymentRef);
        // Hiện tại để trả về payment URl trước, sau này khi có payment method ko trả về url thì sửa sau
        String paymentUrl = paymentStrategy.processPayment(httpReq, createReq);
        createProcessingPayment(createReq);
        return paymentUrl;
    }

    @Override
    public void createProcessingPayment(CreatePaymentRequest createReq) {
        paymentRepository.save(
                Payment.builder()
                    .learnerId(createReq.getLearnerId())
                    .orderId(createReq.getOrderId())
                    .amount(BigDecimal.valueOf(createReq.getAmount()))
                    .currencyCode(createReq.getCurrencyCode())
                    .paymentRef(createReq.getPaymentRef())
                    .paymentMethod(PaymentMethod.valueOf(createReq.getPaymentMethod()))
                    .status(PaymentStatus.PROCESSING)
                    .returnUrl(vnPayConfig.getReturnUrl())
                .build()
        );
    }

    private int countAllowedRetryPayment(String orderId) {
        return paymentRepository.countByOrderIdNotPAID(orderId);
    }

    @Override
    @Transactional
    public Map<String, String> handlePaymentCallback(Map<String, String> params) {
        Map<String, String> fields = new HashMap<>();
        log.info("ℹ️ℹ️ vnpay-ipn is called");
        // Lay cac param cho vao map field cho buoc kiem tra chu ki ben duoi:
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key != null &&  !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                fields.put(key, URLDecoder.decode(value, StandardCharsets.US_ASCII));
            }
        }

        // Hash fields de lay hashData
        String hashData = VnPayUtil.hashAllFields(fields, vnPayConfig.getHashSecret());

        // ⛔ KIỂM TRA BƯỚC 1: Check chữ ký (Hợp lệ hay bị Hacker sửa đổi dữ liệu?)
        if (!hashData.equals(params.get("vnp_SecureHash"))) {
            log.warn("⚠️ Invalid Checksum");
            return buildIpnResponse("97", "Invalid checksum");
        }

        // ⛔ KIỂM TRA BƯỚC 2: Check xem đơn hàng có tồn tại trong hệ thống của mình không?
        Payment payment = getPaymentFromTmnCode(params.get("vnp_TxnRef"));
        if (payment == null) {
            log.warn("⚠️ Payment of Order not found");
            return buildIpnResponse("01", "Payment have not existed yet");
        }
        // ⛔ KIỂM TRA BƯỚC 3: Check xem số tiền VNPAY thu hộ có khớp với giá trị đơn hàng gốc không?
        long amount = Long.parseLong(params.get("vnp_Amount"))/100;
        if (payment.getAmount().longValue() != amount) {
            log.warn("⚠️ Invalid Amount");
            return buildIpnResponse("04", "Payment amount doesn't match");
        }
        // ⛔ KIỂM TRA BƯỚC 4: Check trạng thái đơn hàng hiện tại xem đã được xử lý trước đó chưa?
        if (!payment.getStatus().equals(PaymentStatus.PROCESSING)) {
            log.warn("⚠️ Order already confirmed");
            return buildIpnResponse("02", "Payment have already been processed");
        }

        String responseCode = params.get("vnp_ResponseCode");
        if (!"00".equals(responseCode)) {
            //System.out.println("\033[91;48;2;150;150;150m" + "Payment Error from customer with Response Code: " + responseCode + "\033[0m");
            log.warn("⛔ Cập nhật đơn hàng thành THANH TOÁN THẤT BẠI!");
            processFailedPayment(payment, params);
        } else {
            log.info("✅ Cập nhật đơn hàng thành ĐÃ THANH TOÁN thành công!");
            processSuccessPayment(payment, params);
        }
        return buildIpnResponse("00", "Payment has been processed successfully");
    }

    private Map<String, String> buildIpnResponse (String code, String message) {
        return Map.of("RspCode", code, "Message", message);
    }

    private void processFailedPayment (Payment payment, Map<String, String> params) {
        // xu li payment the nao
        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
        // Ban tin gi
        PaymentProcessMessage msg = PaymentProcessMessage.builder()
                .orderId(payment.getOrderId())
                .learnerId(payment.getLearnerId())
                .transactionNo(params.get("vnp_TransactionNo"))
                .amount(Long.parseLong(params.get("vnp_Amount"))/100)
                .status(PaymentStatus.FAILED.toString())
                .build();
        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_VNPAY_FAILURE_ROUTING_KEY,
                msg
        );
    }

    private void processSuccessPayment (Payment payment, Map<String, String> params) {
        // Cap nhat payment thanh cong va thoi gian
        String vnp_PayDate = params.get("vnp_PayDate");
        Instant paidAt;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime localDateTime = LocalDateTime.parse(vnp_PayDate, formatter);
            paidAt = localDateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        } catch (Exception e) {
            paidAt = Instant.now();
        }

        payment.setPaidAt(paidAt);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // Ban tin
        PaymentProcessMessage msg = PaymentProcessMessage.builder()
                .orderId(payment.getOrderId())
                .amount(Long.parseLong(params.get("vnp_Amount"))/100)
                .learnerId(payment.getLearnerId())
                .status(PaymentStatus.SUCCESS.toString())
                .paidAt(paidAt)
                .transactionNo(params.get("vnp_TransactionNo"))
                .build();

        rabbitTemplate.convertAndSend(
                PAYMENT_EXCHANGE,
                PAYMENT_VNPAY_SUCCESS_ROUTING_KEY,
                msg
        );
    }

    private Payment getPaymentFromTmnCode (String txnRef) {
        return paymentRepository.findPaymentByPaymentRef(txnRef)
                .orElse(null);
    }

}
