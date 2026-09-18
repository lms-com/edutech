package com.lms.finance.service;

import com.lms.finance.config.VnPayConfig;
import com.lms.finance.dto.message.PaymentProcessMessage;
import com.lms.finance.entity.InstructorBalance;
import com.lms.finance.entity.Payment;
import com.lms.finance.enums.PaymentMethod;
import com.lms.finance.enums.PaymentStatus;
import com.lms.finance.repository.InstructorBalanceRepository;
import com.lms.finance.repository.PaymentRepository;
import com.lms.finance.service.impl.PaymentServiceImpl;
import com.lms.finance.util.VnPayUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static com.lms.finance.config.RabbitMQConfig.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaymentServiceTest {
    @Autowired
    private PaymentServiceImpl paymentService;
    @Autowired
    private VnPayConfig vnPayConfig;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private VnPayUtil vnpayUtil;
    @Autowired
    private InstructorBalanceRepository instructorBalanceRepository;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    private String validHashSecret;

    @BeforeEach
    void setup() {
        this.validHashSecret = vnPayConfig.getHashSecret();
        // Cau hinh hanh vi mac dinh cho rabbitTemplate (Khong lam gi ca khi goi gui tin):
        Mockito.doNothing().when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Chèn dữ liệu mồi trực tiếp vào DB H2 trên RAM trước mỗi bài test
        if (!instructorBalanceRepository.existsByInstructorId(("INSTRUCTOR-99"))) {
            instructorBalanceRepository.saveAndFlush(
                    InstructorBalance.builder()
                            .instructorId("INSTRUCTOR-99")
                            .actualBalance(BigDecimal.ZERO)
                            .availableBalance(BigDecimal.ZERO)
                            .blockedBalance(BigDecimal.ZERO)
                            .pendingBalance(BigDecimal.ZERO)
                            .currencyCode("VND")
                            .build()
            );
        }
    }

    // -- Ham helper tao params gui len (Co tinh hash chuan)
    private Map<String, String> createBaseParams (String txnRef, String amount, String responseCode) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_Amount", amount);
        if (responseCode != null) {
            params.put("vnp_ResponseCode", responseCode);
        }
        params.put("vnp_TransactionNo", "TRANS123456");
        params.put("vnp_PayDate", "20260917170000");

        Map<String, String> fields = new HashMap<>();
        for(Map.Entry<String, String> entry : params.entrySet()) {
            fields.put(entry.getKey(), entry.getValue());
        }
        params.put("vnp_SecureHash", vnpayUtil.hashAllFields(fields, validHashSecret));
        return params;
    }

    // =========================================================================
    // 🛠️ THỰC HIỆN 6 TEST CASES LUỒNG LỖI & THÀNH CÔNG
    // =========================================================================
    @Test
    void testHandlePaymentCallback_Case1_InvalidChecksum() {
        // Tao params sai hoan toan:
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "DH001");
        params.put("vnp_Amount", "100");
        params.put("vnp_PayDate", "20260917160000");
        params.put("vnp_SecureHash", "Hello, You Were Hacked. Hahaha!");

        // Thuc thi ham handle callback
        Map<String, String> response = paymentService.handlePaymentCallback(params);

        // Kiem tra ket qua phan hoi loi "97" cho VNPAY:
        Assertions.assertEquals("97", response.get("RspCode"));
        Assertions.assertEquals("Invalid checksum", response.get("Message"));
    }



    @Test
    void testHandelPaymentCallback_Case6_Success() {
        String txnRef = "DH006";
        Map<String, String> params = createBaseParams(txnRef, "150000000", "00");

        //Luu don hang hop le dang PROCESSING vao db H2:
        Payment payment = new Payment();
        payment.setPaymentRef(txnRef);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setAmount(new BigDecimal(1500000L));
        payment.setOrderId("ORDER-666");
        payment.setLearnerId("LEARNER-666");
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        paymentRepository.save(payment);

        // Print status Payment truoc khi xu li:
        System.out.println(String.format("🗨️ Payment %s's status before handling callback",
                paymentRepository.findPaymentByPaymentRef(txnRef).orElseThrow()
                        .getStatus().toString()));

        // Thuc thi:
        Map<String, String> response = paymentService.handlePaymentCallback(params);

        // Print status Payment Sau khi xu li:
        System.out.println(String.format("🆗 Payment %s's status After Handling",
                paymentRepository.findPaymentByPaymentRef(txnRef).orElseThrow()
                        .getStatus().toString()));

        // 1. Kiem tra ket qua phan hoi cho VNPAY
        Assertions.assertEquals("00", response.get("RspCode"));

        // 2. Kiem tra trang thai trong H2 DB: Don hang phai chuyen sang status SUCCESS va cap nhat field paidAt
        Payment updatedPaymet = paymentRepository.findPaymentByPaymentRef(txnRef).orElseThrow();
        Assertions.assertEquals(PaymentStatus.SUCCESS, updatedPaymet.getStatus());
        Assertions.assertNotNull(updatedPaymet.getPaidAt());

        // 3. Kiem tra rabbitMQ co thuc su ban tin nhan thanh cong di khong
        Mockito.verify(rabbitTemplate, Mockito.times(1)).
                convertAndSend(eq(PAYMENT_EXCHANGE), eq(PAYMENT_VNPAY_SUCCESS_ROUTING_KEY), any(PaymentProcessMessage.class));
    }

    @Test
    void testHandlePaymentCallback_Success_VerifyRabbitMessage() {
        String txnRef = "DH006";
        Map<String, String> params = createBaseParams(txnRef, "150000000", "00");

        //Luu don hang hop le dang PROCESSING vao db H2:
        Payment payment = new Payment();
        payment.setPaymentRef(txnRef);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setAmount(new BigDecimal(1500000L));
        payment.setOrderId("ORDER-666");
        payment.setLearnerId("LEARNER-666");
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        paymentRepository.save(payment);

        // 1. Chạy hàm xử lý callback
        paymentService.handlePaymentCallback(params);

        // 2. Tạo Captor để hứng Message truyền vào rabbitTemplate
        ArgumentCaptor<PaymentProcessMessage> messageCaptor = ArgumentCaptor.forClass(PaymentProcessMessage.class);

        // 3. Verify rabbitTemplate đã được gọi và bắt lại tham số truyền vào
        Mockito.verify(rabbitTemplate).convertAndSend(
                eq(PAYMENT_EXCHANGE),
                eq(PAYMENT_VNPAY_SUCCESS_ROUTING_KEY),
                messageCaptor.capture()
        );

        PaymentProcessMessage sentMessage = messageCaptor.getValue();

        // 4. In ra Console để mắt thấy tai nghe
        System.out.println("🚀 MESSAGE SENT TO RABBITMQ: " + sentMessage);

        // 5. Assert dữ liệu của bản tin
        Assertions.assertEquals("ORDER-666", sentMessage.getOrderId());
        Assertions.assertEquals(PaymentStatus.SUCCESS.toString(), sentMessage.getStatus());
        Assertions.assertNotNull(sentMessage.getPaidAt());
    }
}
