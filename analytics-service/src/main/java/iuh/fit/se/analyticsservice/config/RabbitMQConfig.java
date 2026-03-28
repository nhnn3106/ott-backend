package iuh.fit.se.analyticsservice.config;

import org.springframework.amqp.core.Bindinginding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration
 * 
 * Configures queues, exchanges, and bindings for analytics event consumption.
 * 
 * Event Flow:
 * 1. User/Auth Service → analytics.exchange (Topic Exchange)
 * 2. Exchange routes to queues based on routing key
 * 3. Analytics Service consumes from queues
 * 
 * @author OTT Platform Team
 */
@Configuration
public class RabbitMQConfig {

    // Exchange name
    public static final String ANALYTICS_EXCHANGE = "analytics.exchange";
    
    // Queue names
    public static final String USER_REGISTERED_QUEUE = "analytics.user.registered.queue";
    public static final String SESSION_CREATED_QUEUE = "analytics.session.created.queue";
    public static final String USER_LOGIN_QUEUE = "analytics.user.login.queue";
    
    // Routing keys
    public static final String USER_REGISTERED_ROUTING_KEY = "analytics.user.registered";
    public static final String SESSION_CREATED_ROUTING_KEY = "analytics.session.created";
    public static final String USER_LOGIN_ROUTING_KEY = "analytics.user.login";
    
    // Dead Letter Exchange
    public static final String DLX_EXCHANGE = "analytics.dlx.exchange";
    public static final String DLX_QUEUE = "analytics.dlx.queue";
    public static final String DLX_ROUTING_KEY = "analytics.dlx";

    /**
     * Topic Exchange for analytics events
     */
    @Bean
    public TopicExchange analyticsExchange() {
        return ExchangeBuilder
            .topicExchange(ANALYTICS_EXCHANGE)
            .durable(true)
            .build();
    }

    /**
     * Dead Letter Exchange for failed messages
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
            .directExchange(DLX_EXCHANGE)
            .durable(true)
            .build();
    }

    /**
     * Dead Letter Queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
            .durable(DLX_QUEUE)
            .build();
    }

    /**
     * Bind Dead Letter Queue to DLX
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
            .bind(deadLetterQueue())
            .to(deadLetterExchange())
            .with(DLX_ROUTING_KEY);
    }

    /**
     * Queue for user registration events
     */
    @Bean
    public Queue userRegisteredQueue() {
        return QueueBuilder
            .durable(USER_REGISTERED_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build();
    }

    /**
     * Queue for session creation events
     */
    @Bean
    public Queue sessionCreatedQueue() {
        return QueueBuilder
            .durable(SESSION_CREATED_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build();
    }

    /**
     * Queue for user login events
     */
    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder
            .durable(USER_LOGIN_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", DLX_ROUTING_KEY)
            .build();
    }

    /**
     * Bind user registered queue to exchange
     */
    @Bean
    public Binding userRegisteredBinding() {
        return BindingBuilder
            .bind(userRegisteredQueue())
            .to(analyticsExchange())
            .with(USER_REGISTERED_ROUTING_KEY);
    }

    /**
     * Bind session created queue to exchange
     */
    @Bean
    public Binding sessionCreatedBinding() {
        return BindingBuilder
            .bind(sessionCreatedQueue())
            .to(analyticsExchange())
            .with(SESSION_CREATED_ROUTING_KEY);
    }

    /**
     * Bind user login queue to exchange
     */
    @Bean
    public Binding userLoginBinding() {
        return BindingBuilder
            .bind(userLoginQueue())
            .to(analyticsExchange())
            .with(USER_LOGIN_ROUTING_KEY);
    }

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate with JSON converter
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * Listener container factory with JSON converter
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(10);
        factory.setDefaultRequeueRejected(false); // Send to DLX on error
        return factory;
    }
}
