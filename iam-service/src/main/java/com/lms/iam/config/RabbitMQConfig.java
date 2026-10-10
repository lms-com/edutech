package com.lms.iam.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_IAM = "lms.iam.exchange";
    public static final String ROUTING_KEY_OTP = "auth.email.send_otp";

    @Bean
    public TopicExchange iamExchange() {
        return new TopicExchange(EXCHANGE_IAM);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
