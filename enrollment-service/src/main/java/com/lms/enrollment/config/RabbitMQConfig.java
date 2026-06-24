package com.lms.enrollment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Queue chính — lắng nghe order.completed từ Order Service
    @Bean
    public Queue enrollmentOrderCompletedQueue() {
        return QueueBuilder
            .durable("enrollment.order.completed.queue")
            // Nếu xử lý lỗi → chuyển sang DLQ thay vì requeue vô hạn
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", "enrollment.order.completed.queue.dlq")
            .build();
    }

    // Dead Letter Queue — gom message lỗi lại xử lý thủ công sau
    @Bean
    public Queue enrollmentOrderCompletedDlq() {
        return QueueBuilder.durable("enrollment.order.completed.queue.dlq").build();
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange("lms.order.exchange", true, false);
    }

    @Bean
    public Binding enrollmentBinding(Queue enrollmentOrderCompletedQueue,
                                     TopicExchange orderExchange) {
        return BindingBuilder
            .bind(enrollmentOrderCompletedQueue)
            .to(orderExchange)
            .with("order.completed");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
