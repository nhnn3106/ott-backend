package iuh.fit.userservice.service;

import iuh.fit.userservice.config.RabbitMQConfig;
import iuh.fit.userservice.dto.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    public void publishUserCreated(UserCreatedEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("Skip publish user.created event due to missing userId");
            return;
        }

        try {
            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.userEventsExchange,
                    rabbitMQConfig.userCreatedRoutingKey,
                    event
            );
            log.info("Published user.created event for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish user.created event for userId={}", event.getUserId(), e);
        }
    }
}
