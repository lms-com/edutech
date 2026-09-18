package com.lms.order.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    /**
     * Receiving Message
     */
    // ********************************************************//
    // ********************************************************//
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_VNPAY_SUCCESS_ROUTING_KEY = "payment.vnpay.success";

    @Bean
    public Queue paymentProcessedQueue() {
        return new Queue(PAYMENT_PROCESSED_QUEUE, true);
    }

    @Bean
    public Binding paymentSuccessBinding(Queue paymentProcessedQueue) {
        TopicExchange paymentExchange = new TopicExchange(PAYMENT_EXCHANGE);
        return BindingBuilder.bind(paymentProcessedQueue).to(paymentExchange).with(PAYMENT_VNPAY_SUCCESS_ROUTING_KEY);
    }

    /**
     * BroadCasting Message
     */
    // ********************************************************//
    // ********************************************************//
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }


    /**
     * Cau hinh Converter va Template
     */
    // ********************************************************//
    // ********************************************************//
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return rabbitTemplate;
    }
}
