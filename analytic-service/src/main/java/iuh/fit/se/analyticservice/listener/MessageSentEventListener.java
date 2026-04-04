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
import iuh.fit.se.analyticservice.entity.RawMessageEvent;
import iuh.fit.se.analyticservice.repository.RawMessageEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSentEventListener {

    private final RawMessageEventRepository rawMessageEventRepository;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    @RabbitListener(queues = RabbitMqConfig.MESSAGE_SENT_QUEUE)
    public void handleMessageSent(Message message) {
        try {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            Map<String, Object> data = jsonParser.parseMap(payload);

            RawMessageEvent raw = new RawMessageEvent(
                    String.valueOf(data.get("eventId")),
                    String.valueOf(data.get("messageId")),
                    String.valueOf(data.get("userId")),
                    String.valueOf(data.get("messageType")),
                    Instant.parse(String.valueOf(data.get("timestamp")))
            );

            rawMessageEventRepository.save(raw);
            log.info("Saved message event: eventId={}, messageType={}", raw.getEventId(), raw.getMessageType());
        } catch (Exception ex) {
            log.error("Failed to parse/save message sent event", ex);
        }
    }
}
