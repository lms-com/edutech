package com.lms.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String MEDIA_EXCHANGE = "media.exchange";
    public static final String VIDEO_PROCESSING_QUEUE =  "video.processing.queue";
    public static final String VIDEO_COMPLETED_QUEUE =  "video.completed.queue";

    public static final String VIDEO_PROCESSING_ROUTING_KEY = "video.processing.routing.key";
    public static final String VIDEO_COMPLETED_ROUTING_KEY =  "video.completed.routing.key";

    @Bean
    public DirectExchange mediaExchange() {
        return new DirectExchange(MEDIA_EXCHANGE, true, false);
    }

    @Bean
    public Queue videoProcessingQueue() {
        return new Queue(VIDEO_PROCESSING_QUEUE, true);
    }

    @Bean
    public Binding videoProcessingBinding(Queue videoProcessingQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(videoProcessingQueue)
                .to(mediaExchange)
                .with(VIDEO_PROCESSING_ROUTING_KEY);
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
