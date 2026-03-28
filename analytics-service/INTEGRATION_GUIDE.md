# 🔗 Integration Guide - Analytics Service

This guide explains how to integrate User Service and Auth Service with Analytics Service through RabbitMQ event publishing.

---

## 📋 Overview

Analytics Service consumes events from:

- **User Service**: User registration, session creation
- **Auth Service**: Login attempts

All events must include an `eventId` field for idempotency.

---

## 🚀 Step 1: Add RabbitMQ Dependencies

### User Service & Auth Service - pom.xml

```xml
<!-- Add to pom.xml if not already present -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

---

## 🎯 Step 2: Update User Service

### 2.1 Create Event DTOs

**File**: `user-service/src/main/java/iuh/fit/userservice/dto/event/UserRegisteredEvent.java`

```java
package iuh.fit.userservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEvent implements Serializable {

    private String eventId;  // REQUIRED for idempotency
    private UUID userId;
    private String phone;
    private String email;
    private String registrationMethod;  // "GOOGLE" or "LOCAL"

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
```

**File**: `user-service/src/main/java/iuh/fit/userservice/dto/event/SessionCreatedEvent.java`

```java
package iuh.fit.userservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionCreatedEvent implements Serializable {

    private String eventId;  // REQUIRED for idempotency
    private UUID userId;
    private UUID sessionId;
    private String deviceType;  // MOBILE, DESKTOP, TABLET, TV, UNKNOWN
    private String loginMethod;  // LOCAL, GOOGLE, OTP, QR_CODE
    private String ipAddress;
    private String location;  // e.g., "Ho Chi Minh City, Vietnam"

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
```

### 2.2 Create Event Publisher Service

**File**: `user-service/src/main/java/iuh/fit/userservice/service/EventPublisherService.java`

```java
package iuh.fit.userservice.service;

import iuh.fit.userservice.dto.event.SessionCreatedEvent;
import iuh.fit.userservice.dto.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    private static final String ANALYTICS_EXCHANGE = "analytics.exchange";
    private static final String USER_REGISTERED_KEY = "analytics.user.registered";
    private static final String SESSION_CREATED_KEY = "analytics.session.created";

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            // Generate eventId if not present
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }

            rabbitTemplate.convertAndSend(ANALYTICS_EXCHANGE, USER_REGISTERED_KEY, event);
            log.info("Published UserRegisteredEvent: eventId={}, userId={}",
                event.getEventId(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to publish UserRegisteredEvent", e);
        }
    }

    public void publishSessionCreated(SessionCreatedEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }

            rabbitTemplate.convertAndSend(ANALYTICS_EXCHANGE, SESSION_CREATED_KEY, event);
            log.info("Published SessionCreatedEvent: eventId={}, userId={}, sessionId={}",
                event.getEventId(), event.getUserId(), event.getSessionId());
        } catch (Exception e) {
            log.error("Failed to publish SessionCreatedEvent", e);
        }
    }
}
```

### 2.3 Update UserServiceImpl

**Modify**: `user-service/src/main/java/iuh/fit/userservice/service/impl/UserServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    // ... existing dependencies ...
    private final EventPublisherService eventPublisherService;

    @Override
    @Transactional
    public UserResponse register(UserRegisterRequest request) {
        // ... existing registration logic ...
        User savedUser = userRepository.save(user);

        // ✅ PUBLISH EVENT TO ANALYTICS
        UserRegisteredEvent event = UserRegisteredEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(savedUser.getId())
            .phone(savedUser.getPhone())
            .email(savedUser.getEmail())
            .registrationMethod(savedUser.getProvider())  // "GOOGLE" or "LOCAL"
            .timestamp(LocalDateTime.now())
            .build();

        eventPublisherService.publishUserRegistered(event);

        return UserMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public SessionResponse createSession(UUID userId, String deviceType, String ipAddress) {
        // ... existing session creation logic ...
        Session savedSession = sessionRepository.save(session);

        // ✅ PUBLISH EVENT TO ANALYTICS
        SessionCreatedEvent event = SessionCreatedEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .userId(userId)
            .sessionId(savedSession.getId())
            .deviceType(deviceType)
            .loginMethod("LOCAL")  // or extract from context
            .ipAddress(ipAddress)
            .location(extractLocationFromIp(ipAddress))  // implement or use "Unknown"
            .timestamp(LocalDateTime.now())
            .build();

        eventPublisherService.publishSessionCreated(event);

        return SessionMapper.toSessionResponse(savedSession);
    }

    private String extractLocationFromIp(String ipAddress) {
        // TODO: Implement IP geolocation (e.g., using MaxMind GeoIP2)
        // For now, return default
        return "Unknown";
    }
}
```

---

## 🔐 Step 3: Update Auth Service

### 3.1 Create Event DTO

**File**: `auth-service/src/main/java/iuh/fit/authservice/dto/event/UserLoginEvent.java`

```java
package iuh.fit.authservice.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginEvent implements Serializable {

    private String eventId;  // REQUIRED for idempotency
    private UUID userId;
    private String loginMethod;  // LOCAL, GOOGLE, OTP, QR_CODE
    private String status;  // SUCCESS, FAILED, BLOCKED, REQUIRES_2FA
    private String deviceType;  // MOBILE, DESKTOP, TABLET, TV, UNKNOWN
    private String ipAddress;
    private String location;
    private String failureReason;  // Only if status is FAILED

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}
```

### 3.2 Create Event Publisher Service

**File**: `auth-service/src/main/java/iuh/fit/authservice/service/EventPublisherService.java`

```java
package iuh.fit.authservice.service;

import iuh.fit.authservice.dto.event.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    private static final String ANALYTICS_EXCHANGE = "analytics.exchange";
    private static final String USER_LOGIN_KEY = "analytics.user.login";

    public void publishUserLogin(UserLoginEvent event) {
        try {
            if (event.getEventId() == null) {
                event.setEventId(UUID.randomUUID().toString());
            }

            rabbitTemplate.convertAndSend(ANALYTICS_EXCHANGE, USER_LOGIN_KEY, event);
            log.info("Published UserLoginEvent: eventId={}, userId={}, status={}",
                event.getEventId(), event.getUserId(), event.getStatus());
        } catch (Exception e) {
            log.error("Failed to publish UserLoginEvent", e);
        }
    }
}
```

### 3.3 Update AuthServiceImpl

**Modify**: `auth-service/src/main/java/iuh/fit/authservice/service/impl/AuthServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    // ... existing dependencies ...
    private final EventPublisherService eventPublisherService;

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            // ... existing authentication logic ...

            // On successful login
            User user = authenticate(request);

            // ✅ PUBLISH SUCCESS EVENT
            UserLoginEvent event = UserLoginEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(user.getId())
                .loginMethod("LOCAL")
                .status("SUCCESS")
                .deviceType(extractDeviceType(request))
                .ipAddress(request.getIpAddress())
                .location("Unknown")
                .timestamp(LocalDateTime.now())
                .build();

            eventPublisherService.publishUserLogin(event);

            return generateLoginResponse(user);

        } catch (BadCredentialsException e) {
            // ✅ PUBLISH FAILED EVENT
            UserLoginEvent event = UserLoginEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .userId(null)  // User not found
                .loginMethod("LOCAL")
                .status("FAILED")
                .deviceType(extractDeviceType(request))
                .ipAddress(request.getIpAddress())
                .location("Unknown")
                .failureReason("Invalid credentials")
                .timestamp(LocalDateTime.now())
                .build();

            eventPublisherService.publishUserLogin(event);

            throw e;
        }
    }

    private String extractDeviceType(LoginRequest request) {
        String userAgent = request.getUserAgent();
        if (userAgent == null) return "UNKNOWN";

        if (userAgent.contains("Mobile")) return "MOBILE";
        if (userAgent.contains("Tablet")) return "TABLET";
        if (userAgent.contains("TV")) return "TV";
        return "DESKTOP";
    }
}
```

---

## 🔧 Step 4: RabbitMQ Configuration

### Both Services - application.properties

```properties
# RabbitMQ Configuration
spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

---

## ✅ Verification

### 1. Check RabbitMQ Queues

```bash
# List queues
rabbitmqctl list_queues

# Expected output:
analytics.user.registered.queue    0
analytics.session.created.queue    0
analytics.user.login.queue         0
analytics.dlx.queue               0
```

### 2. Test Event Publishing

```bash
# Register a new user
POST http://localhost:8082/api/users/register
{
  "phone": "+84987654321",
  "email": "test@example.com",
  "password": "Password123!",
  "name": "Test User"
}

# Check Analytics Service logs
docker logs analytics-service | grep "UserRegisteredEvent"
# Expected: "Received UserRegisteredEvent: eventId=xxx, userId=yyy"
```

### 3. Verify Database

```sql
-- Check raw events
SELECT * FROM raw_events_log ORDER BY created_at DESC LIMIT 10;

-- Check today's counters
-- In Analytics Service, call:
GET http://localhost:8084/api/analytics/realtime/today
```

---

## 🐛 Troubleshooting

### Issue: Events not reaching Analytics Service

```bash
# Check RabbitMQ exchange exists
rabbitmqadmin list exchanges

# Check bindings
rabbitmqadmin list bindings

# Check User/Auth Service logs for publish errors
docker logs user-service | grep "Failed to publish"
```

### Issue: Duplicate events in database

```bash
# Check Redis for idempotency keys
redis-cli KEYS "event:processed:*"

# Verify eventId is being generated
# Check raw_events_log for duplicate event_id
SELECT event_id, COUNT(*) FROM raw_events_log GROUP BY event_id HAVING COUNT(*) > 1;
```

---

## 📝 Checklist

### User Service Integration

- [ ] Added RabbitMQ dependency
- [ ] Created Event DTOs (UserRegisteredEvent, SessionCreatedEvent)
- [ ] Created EventPublisherService
- [ ] Updated registration logic to publish UserRegisteredEvent
- [ ] Updated session creation to publish SessionCreatedEvent
- [ ] Tested event publishing

### Auth Service Integration

- [ ] Added RabbitMQ dependency
- [ ] Created Event DTO (UserLoginEvent)
- [ ] Created EventPublisherService
- [ ] Updated login logic to publish UserLoginEvent (success + failed)
- [ ] Tested event publishing

### Verification

- [ ] RabbitMQ queues created automatically by Analytics Service
- [ ] Events appearing in Analytics Service logs
- [ ] Events stored in raw_events_log table
- [ ] Real-time metrics updating in Redis
- [ ] Daily metrics calculating correctly

---

## 📞 Support

If you encounter issues:

1. Check Analytics Service logs: `docker logs analytics-service`
2. Check RabbitMQ management UI: `http://localhost:15672`
3. Verify database connections
4. Ensure all services can reach RabbitMQ

---

**Last Updated**: 2024-01-01
