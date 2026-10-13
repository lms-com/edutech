package com.lms.finance.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    /**
     * Broadcasting message
     */
    //public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_VNPAY_SUCCESS_ROUTING_KEY = "payment.vnpay.success";
    public static final String PAYMENT_VNPAY_FAILURE_ROUTING_KEY = "payment.vnpay.failure";

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE, true, false);
    }

    public static final String PAYOUT_EXCHANGE = "payout.exchange";
    public static final String PAYOUT_PENDING_ROUTING_KEY = "payout.pending";
    public static final String PAYOUT_SUCCESS_ROUTING_KEY = "payout.success";
    public static final String PAYOUT_REJECTED_ROUTING_KEY = "payout.rejected";

    @Bean
    public TopicExchange payoutExchange() {
        return new TopicExchange(PAYOUT_EXCHANGE, true, false);
    }

    /**
     * Receiving message
     */
    public static final String ORDER_COMPLETED_QUEUE = "order.completed.queue";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";

    @Bean
    public Queue orderCompletedQueue() {
        return new Queue(ORDER_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding bindingOrderCompleted(Queue orderCompletedQueue) {
        DirectExchange orderExchange = new DirectExchange(ORDER_EXCHANGE);
        return BindingBuilder.bind(orderCompletedQueue).to(orderExchange).with(ORDER_COMPLETED_ROUTING_KEY);
    }

    public static final String EXCHANGE_IAM = "lms.iam.exchange";
    public static final String INSTRUCTOR_APPROVED_FINANCE_QUEUE = "finance.instructor.approved.queue";
    public static final String ROUTING_KEY_INSTRUCTOR_APPROVED = "instructor.application.approved";

    @Bean
    public Queue instructorApprovedFinanceQueue() {
        return new Queue(INSTRUCTOR_APPROVED_FINANCE_QUEUE, true);
    }

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_IAM, true, false);
    }

    @Bean
    public Binding bindingInstructorApproved(Queue instructorApprovedFinanceQueue, TopicExchange iamExchange) {
        return BindingBuilder.bind(instructorApprovedFinanceQueue).to(iamExchange).with(ROUTING_KEY_INSTRUCTOR_APPROVED);
    }


    /**
     * Cau hinh Converter va Template
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        typeMapper.setTrustedPackages("*");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}
