package com.lms.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeTypes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * order.exchange do order-service khai báo là DirectExchange. Khai báo lệch tên
 * hoặc lệch loại ở phía consumer sẽ khiến RabbitMQ từ chối (PRECONDITION_FAILED)
 * hoặc bản tin không bao giờ tới.
 */
class RabbitMQConfigTest {

    @Test
    void orderExchangePhaiKhopKhaiBaoCuaOrderService() {
        DirectExchange exchange = new RabbitMQConfig().orderExchange();

        assertThat(exchange.getName()).isEqualTo("order.exchange");
        assertThat(exchange.getType()).isEqualTo(ExchangeTypes.DIRECT);
        assertThat(exchange.isDurable()).isTrue();
    }

    @Test
    void bindingOrderDungRoutingKeyOrderCompleted() {
        assertThat(new RabbitMQConfig().bindingOrder().getRoutingKey()).isEqualTo("order.completed");
        assertThat(new RabbitMQConfig().bindingOrder().getExchange()).isEqualTo("order.exchange");
    }
}
