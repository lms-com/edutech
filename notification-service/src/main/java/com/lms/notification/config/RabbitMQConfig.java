package com.lms.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. Tên Queue nhận OTP (Chỉ phục vụ riêng cho notification-service)
    public static final String QUEUE_NOTIFICATION_OTP = "lms.notification.queue.auth.otp";

    // 2. Tên Exchange của IAM Service (Trạm điều hướng tin nhắn trung tâm của
    // module IAM)
    public static final String EXCHANGE_IAM = "lms.iam.exchange";

    // Haằng số cho luồng đơn hàng
    public static final String QUEUE_NOTIFICATION_ORDER = "lms.notification.queue.order.completed";
    public static final String EXCHANGE_ORDER = "lms.order.exchange";
    public static final String ROUTING_KEY_ORDER = "order.completed";

    // Khai báo Hàng đợi (Durable = true giúp tin nhắn không bị mất nếu server
    // RabbitMQ bị restart)
    @Bean
    public Queue otpQueue() {
        return new Queue(QUEUE_NOTIFICATION_OTP, true);
    }

    @Bean
    public Queue orderQueue() {
        return new Queue(QUEUE_NOTIFICATION_ORDER, true);
    }

    // Khai báo Topic Exchange để lắng nghe sự kiện từ IAM Service
    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_IAM);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    // 3. Đấu nối (Binding) Queue vào Exchange thông qua Routing Key chuẩn:
    // "auth.email.send_otp"
    @Bean
    public Binding bindingOtp() {
        return BindingBuilder
                .bind(otpQueue())
                .to(iamExchange())
                .with("auth.email.send_otp");
    }

    @Bean
    public Binding bindingOrder() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with(ROUTING_KEY_ORDER);
    }

    // Cấu hình bộ chuyển đổi để Spring tự động dịch chuỗi dữ liệu JSON từ Queue
    // thành Object Java (SendOtpEvent)
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}