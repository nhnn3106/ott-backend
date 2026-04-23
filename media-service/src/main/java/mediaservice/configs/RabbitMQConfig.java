package mediaservice.configs;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange mediaCompressionExchange(MediaCompressionProperties properties) {
        return new DirectExchange(properties.getExchange());
    }

    @Bean
    public Queue mediaCompressionQueue(MediaCompressionProperties properties) {
        return new Queue(properties.getQueue());
    }

    @Bean
    public Binding mediaCompressionBinding(
            Queue mediaCompressionQueue,
            DirectExchange mediaCompressionExchange,
            MediaCompressionProperties properties) {
        return BindingBuilder.bind(mediaCompressionQueue)
                .to(mediaCompressionExchange)
                .with(properties.getRoutingKey());
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
