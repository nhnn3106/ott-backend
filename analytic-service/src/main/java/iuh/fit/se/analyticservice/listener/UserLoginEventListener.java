package iuh.fit.se.analyticservice.listener;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Component;

import iuh.fit.se.analyticservice.config.RabbitMqConfig;
import iuh.fit.se.analyticservice.entity.RawLoginEvent;
import iuh.fit.se.analyticservice.repository.RawLoginEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserLoginEventListener {

    private final RawLoginEventRepository rawLoginEventRepository;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @RabbitListener(queues = RabbitMqConfig.USER_LOGIN_QUEUE)
    public void handleUserLoginEvent(Message message) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> data = jsonParser.parseMap(payload);

            String eventId = String.valueOf(data.get("eventId"));
            String userId = String.valueOf(data.get("userId"));
            String loginMethod = String.valueOf(data.get("loginMethod"));
            Instant timestamp = Instant.parse(String.valueOf(data.get("timestamp")));

            RawLoginEvent raw = new RawLoginEvent(
                    eventId,
                    userId,
                    loginMethod,
                    timestamp
            );

            rawLoginEventRepository.save(raw);
            log.info("Saved login event: eventId={}, userId={}", eventId, userId);
        } catch (Exception ex) {
            log.error("Failed to parse/save login event payload", ex);
        }
    }
}