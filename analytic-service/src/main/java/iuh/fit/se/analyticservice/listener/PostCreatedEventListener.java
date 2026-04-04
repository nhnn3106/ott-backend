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
import iuh.fit.se.analyticservice.entity.RawPostEvent;
import iuh.fit.se.analyticservice.repository.RawPostEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostCreatedEventListener {

    private final RawPostEventRepository rawPostEventRepository;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @RabbitListener(queues = RabbitMqConfig.POST_CREATED_QUEUE)
    public void handlePostCreated(Message message) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> data = jsonParser.parseMap(payload);

            RawPostEvent raw = new RawPostEvent(
                    String.valueOf(data.get("eventId")),
                    String.valueOf(data.get("postId")),
                    String.valueOf(data.get("userId")),
                    Instant.parse(String.valueOf(data.get("timestamp")))
            );

            rawPostEventRepository.save(raw);
            log.info("Saved post event: eventId={}, postId={}", raw.getEventId(), raw.getPostId());
        } catch (Exception ex) {
            log.error("Failed to parse/save post created event", ex);
        }
    }
}
