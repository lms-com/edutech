package com.lms.order.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lms.order.config.RabbitMQConfig;
import com.lms.order.dto.message.PaymentProcessMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SmartMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProcessMessageDeserializationTest {

    private MessageConverter orderServiceConverter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();
        RabbitMQConfig config = new RabbitMQConfig();
        orderServiceConverter = config.jackson2JsonMessageConverter(objectMapper);
    }

    private PaymentProcessMessage deserialize(String json, String typeIdHeader) {
        MessageProperties properties = new MessageProperties();
        properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        if (typeIdHeader != null) {
            properties.setHeader("__TypeId__", typeIdHeader);
        }
        properties.setInferredArgumentType(PaymentProcessMessage.class);
        Message message = new Message(json.getBytes(StandardCharsets.UTF_8), properties);
        Object result = ((SmartMessageConverter) orderServiceConverter).fromMessage(message, PaymentProcessMessage.class);
        assertNotNull(result);
        assertInstanceOf(PaymentProcessMessage.class, result);
        return (PaymentProcessMessage) result;
    }

    @Test
    @DisplayName("1. Deserialization: Decimal epoch timestamp with nanoseconds precision")
    void testDecimalEpochWithNanoseconds() {
        String json = """
                {
                    "orderId": "ORD-001",
                    "learnerId": "L-001",
                    "amount": 500000,
                    "status": "SUCCESS",
                    "transactionNo": "VNP111",
                    "paidAt": 1728198765.123456789
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertEquals("ORD-001", msg.getOrderId());
        assertEquals("L-001", msg.getLearnerId());
        assertEquals(500000L, msg.getAmount());
        assertEquals("SUCCESS", msg.getStatus());
        assertEquals("VNP111", msg.getTransactionNo());
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
        assertEquals(123456789, msg.getPaidAt().getNano());
    }

    @Test
    @DisplayName("2. Deserialization: Decimal epoch timestamp with milliseconds precision (.123)")
    void testDecimalEpochWithMilliseconds() {
        String json = """
                {
                    "orderId": "ORD-002",
                    "paidAt": 1728198765.123
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
        assertEquals(123000000, msg.getPaidAt().getNano());
    }

    @Test
    @DisplayName("3. Deserialization: Decimal epoch timestamp with trailing zero (.0)")
    void testDecimalEpochWithTrailingZero() {
        String json = """
                {
                    "orderId": "ORD-003",
                    "paidAt": 1728198765.0
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
        assertEquals(0, msg.getPaidAt().getNano());
    }

    @Test
    @DisplayName("4. Deserialization: Integer epoch timestamp in seconds")
    void testIntegerEpochSeconds() {
        String json = """
                {
                    "orderId": "ORD-004",
                    "paidAt": 1728198765
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
        assertEquals(0, msg.getPaidAt().getNano());
    }

    @Test
    @DisplayName("5. Deserialization: ISO-8601 string format UTC")
    void testIso8601String() {
        String json = """
                {
                    "orderId": "ORD-005",
                    "paidAt": "2024-10-06T07:12:45.123456789Z"
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNotNull(msg.getPaidAt());
        assertEquals(Instant.parse("2024-10-06T07:12:45.123456789Z"), msg.getPaidAt());
    }

    @Test
    @DisplayName("6. Deserialization: Scientific notation decimal epoch (1.728198765E9)")
    void testScientificNotationEpoch() {
        String json = """
                {
                    "orderId": "ORD-006",
                    "paidAt": 1.728198765E9
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
    }

    @Test
    @DisplayName("7. Deserialization: Null paidAt field")
    void testNullPaidAt() {
        String json = """
                {
                    "orderId": "ORD-007",
                    "paidAt": null
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNull(msg.getPaidAt());
    }

    @Test
    @DisplayName("8. Deserialization: Missing paidAt property entirely")
    void testMissingPaidAt() {
        String json = """
                {
                    "orderId": "ORD-008",
                    "status": "SUCCESS"
                }
                """;
        PaymentProcessMessage msg = deserialize(json, null);
        assertNull(msg.getPaidAt());
        assertEquals("ORD-008", msg.getOrderId());
    }

    @Test
    @DisplayName("9. Deserialization: Type precedence INFERRED overrides finance-service __TypeId__")
    void testTypePrecedenceInferredWithFinanceHeader() {
        String json = """
                {
                    "orderId": "ORD-009",
                    "paidAt": 1728198765.987654321
                }
                """;
        PaymentProcessMessage msg = deserialize(json, "com.lms.finance.dto.message.PaymentProcessMessage");
        assertEquals("ORD-009", msg.getOrderId());
        assertNotNull(msg.getPaidAt());
        assertEquals(1728198765L, msg.getPaidAt().getEpochSecond());
        assertEquals(987654321, msg.getPaidAt().getNano());
    }
}
