package iuh.fit.se.analyticsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.analyticsservice.config.RabbitMQConfig;
import iuh.fit.se.analyticsservice.dto.event.SessionCreatedEvent;
import iuh.fit.se.analyticsservice.dto.event.UserLoginEvent;
import iuh.fit.se.analyticsservice.dto.event.UserRegisteredEvent;
import iuh.fit.se.analyticsservice.entity.RawEventLog;
import iuh.fit.se.analyticsservice.repository.RawEventLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Event Consumer Service
 * 
 * Consumes events from RabbitMQ and stores them in raw_events_log table.
 * Implements idempotency pattern using Redis to prevent duplicate processing.
 * 
 * @author OTT Platform Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumerService {

    private final RawEventLogRepository rawEventLogRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String IDEMPOTENCY_KEY_PREFIX = "event:processed:";
    private static final long IDEMPOTENCY_TTL_HOURS = 24;

    /**
     * Consume User Registered events
     */
    @RabbitListener(queues = RabbitMQConfig.USER_REGISTERED_QUEUE)
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Received UserRegisteredEvent: eventId={}, userId={}", 
            event.getEventId(), event.getUserId());

        // Check idempotency
        if (isEventProcessed(event.getEventId())) {
            log.warn("Event {} already processed, skipping", event.getEventId());
            return;
        }

        try {
            // Store raw event in database
            RawEventLog rawEvent = RawEventLog.builder()
                .eventId(event.getEventId())
                .eventType("USER_REGISTERED")
                .userId(event.getUserId())
                .payload(objectMapper.valueToTree(event))
                .processed(false)
                .build();

            rawEventLogRepository.save(rawEvent);

            // Update real-time counters in Redis
            redisTemplate.opsForValue().increment("analytics:today:registrations", 1);
            
            // Mark event as processed
            markEventAsProcessed(event.getEventId());

            log.info("Successfully processed UserRegisteredEvent: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process UserRegisteredEvent: eventId={}", event.getEventId(), e);
            throw e; // Trigger retry or send to DLX
        }
    }

    /**
     * Consume Session Created events
     */
    @RabbitListener(queues = RabbitMQConfig.SESSION_CREATED_QUEUE)
    @Transactional
    public void handleSessionCreated(SessionCreatedEvent event) {
        log.info("Received SessionCreatedEvent: eventId={}, userId={}, sessionId={}", 
            event.getEventId(), event.getUserId(), event.getSessionId());

        if (isEventProcessed(event.getEventId())) {
            log.warn("Event {} already processed, skipping", event.getEventId());
            return;
        }

        try {
            RawEventLog rawEvent = RawEventLog.builder()
                .eventId(event.getEventId())
                .eventType("SESSION_CREATED")
                .userId(event.getUserId())
                .payload(objectMapper.valueToTree(event))
                .processed(false)
                .build();

            rawEventLogRepository.save(rawEvent);

            // Update active users set in Redis
            String activeUsersKey = "analytics:active:users";
            redisTemplate.opsForSet().add(activeUsersKey, event.getUserId().toString());
            redisTemplate.expire(activeUsersKey, 1, TimeUnit.HOURS);

            markEventAsProcessed(event.getEventId());

            log.info("Successfully processed SessionCreatedEvent: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process SessionCreatedEvent: eventId={}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Consume User Login events
     */
    @RabbitListener(queues = RabbitMQConfig.USER_LOGIN_QUEUE)
    @Transactional
    public void handleUserLogin(UserLoginEvent event) {
        log.info("Received UserLoginEvent: eventId={}, userId={}, status={}", 
            event.getEventId(), event.getUserId(), event.getStatus());

        if (isEventProcessed(event.getEventId())) {
            log.warn("Event {} already processed, skipping", event.getEventId());
            return;
        }

        try {
            RawEventLog rawEvent = RawEventLog.builder()
                .eventId(event.getEventId())
                .eventType("USER_LOGIN")
                .userId(event.getUserId())
                .payload(objectMapper.valueToTree(event))
                .processed(false)
                .build();

            rawEventLogRepository.save(rawEvent);

            // Update login counters in Redis
            String loginKey = "analytics:today:logins:" + event.getStatus();
            redisTemplate.opsForValue().increment(loginKey, 1);

            markEventAsProcessed(event.getEventId());

            log.info("Successfully processed UserLoginEvent: eventId={}", event.getEventId());

        } catch (Exception e) {
            log.error("Failed to process UserLoginEvent: eventId={}", event.getEventId(), e);
            throw e;
        }
    }

    /**
     * Check if event has already been processed (idempotency check)
     */
    private boolean isEventProcessed(String eventId) {
        String key = IDEMPOTENCY_KEY_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Mark event as processed in Redis with TTL
     */
    private void markEventAsProcessed(String eventId) {
        String key = IDEMPOTENCY_KEY_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "1", IDEMPOTENCY_TTL_HOURS, TimeUnit.HOURS);
    }
}
