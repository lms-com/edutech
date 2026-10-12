package com.lms.analytics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";

    // Hàng đợi DÀNH RIÊNG cho việc phân tích
    public static final String ANALYTICS_ORDER_COMPLETED_QUEUE = "analytics.order.completed.queue";

    /*@Bean
    public Queue analyticsOrderCompletedQueue() {
        return new Queue(ANALYTICS_ORDER_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding bindingAnalyticsOrderCompleted(Queue analyticsOrderCompletedQueue) {
        DirectExchange orderExchange = new DirectExchange(ORDER_EXCHANGE);
        return BindingBuilder.bind(analyticsOrderCompletedQueue)
                .to(orderExchange)
                .with(ORDER_COMPLETED_ROUTING_KEY);
    }*/

    // --- Giữ nguyên cấu hình Converter json giống file của Finance ---
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");

        // THÊM ĐOẠN NÀY ĐỂ ÉP KIỂU KHÉP KÍN, TRỊ LỖI CLASSNOTFOUND
        Map<String, Class<?>> idClassMapping = new HashMap<>();
        idClassMapping.put("com.lms.course.dto.message.CourseSyncEventMessage", com.lms.analytics.dto.message.CourseSyncEventMessage.class);
        typeMapper.setIdClassMapping(idClassMapping);

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();

        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);

        // 1. TẮT TÍNH NĂNG NHAI LẠI VÔ TẬN
        factory.setDefaultRequeueRejected(false);

        // 2. HARDCODE CHUẨN XÁC LUỒNG RETRY VÀ DLQ
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)                                 // Thử tối đa 3 lần
                .backOffOptions(2000, 2.0, 10000)               // Lần 1 cách lần 2 là 2s, nhân đôi lên
                .recoverer(new RejectAndDontRequeueRecoverer()) // Nếu thất bại 3 lần -> Bắn thẳng sang DLX (Thùng rác)
                .build());

        return factory;
    }
}
