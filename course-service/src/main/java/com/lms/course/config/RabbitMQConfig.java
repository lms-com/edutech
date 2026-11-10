package com.lms.course.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MEDIA_EXCHANGE = "media.exchange";
    public static final String VIDEO_COMPLETED_QUEUE = "video.completed.queue";
    public static final String VIDEO_COMPLETED_ROUTING_KEY = "video.completed.routing.key";

    // Hằng số cho luồng duyệt / từ chối khóa học (Thông báo cho Giảng viên)
    public static final String EXCHANGE_COURSE = "lms.course.exchange";
    public static final String ROUTING_KEY_COURSE_STATUS = "course.status.changed";

    @Bean
    public DirectExchange mediaExchange() {
        return new DirectExchange(MEDIA_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange courseStatusExchange() {
        return new TopicExchange(EXCHANGE_COURSE, true, false);
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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
