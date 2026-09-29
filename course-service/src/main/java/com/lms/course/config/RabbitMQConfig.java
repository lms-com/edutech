package com.lms.course.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MEDIA_EXCHANGE = "media.exchange";
    public static final String VIDEO_COMPLETED_QUEUE = "video.completed.queue";
    public static final String VIDEO_COMPLETED_ROUTING_KEY = "video.completed.routing.key";

    @Bean
    public DirectExchange mediaExchange() {
        return new DirectExchange(MEDIA_EXCHANGE, true, false);
    }

    @Bean
    public Queue videoCompletedQueue() {
        return new Queue(VIDEO_COMPLETED_QUEUE, true);
    }

    @Bean
    public Binding videoCompletedBinding(Queue videoCompletedQueue, DirectExchange mediaExchange) {
        return BindingBuilder.bind(videoCompletedQueue)
                .to(mediaExchange)
                .with(VIDEO_COMPLETED_ROUTING_KEY);
    }
}
