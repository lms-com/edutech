package com.lms.enrollment.config;

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
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange", true, false);
    }

    @Bean
    public TopicExchange courseCompletedExchange() {
        return new TopicExchange("course-exchange", true, false);
    }

    @Bean
    public Binding enrollmentBinding(Queue enrollmentOrderCompletedQueue,
                                     DirectExchange orderExchange) {
        return BindingBuilder
            .bind(enrollmentOrderCompletedQueue)
            .to(orderExchange)
            .with("order.completed");
    }

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
