package iuh.fit.notificationservice.service;

import iuh.fit.notificationservice.dto.request.PushTokenRequest;
import iuh.fit.notificationservice.entity.InAppNotification;
import iuh.fit.notificationservice.entity.PushToken;
import iuh.fit.notificationservice.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final PushTokenRepository pushTokenRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void registerToken(PushTokenRequest request) {
        String token = normalize(request.getToken());
        if (!isExpoPushToken(token)) {
            log.warn("Ignored invalid Expo push token for userId={}", request.getUserId());
            return;
        }

        PushToken pushToken = pushTokenRepository.findByExpoPushToken(token)
                .orElseGet(PushToken::new);

        pushToken.setUserId(request.getUserId());
        pushToken.setExpoPushToken(token);
        pushToken.setPlatform(normalize(request.getPlatform()));
        pushToken.setDeviceId(normalize(request.getDeviceId()));
        pushToken.setActive(true);

        pushTokenRepository.save(pushToken);
    }

    @Transactional
    public void unregisterToken(PushTokenRequest request) {
        String token = normalize(request.getToken());
        if (token == null) return;

        pushTokenRepository.findByExpoPushToken(token).ifPresent(pushToken -> {
            pushToken.setActive(false);
            pushTokenRepository.save(pushToken);
        });
    }

    public void sendNotification(InAppNotification notification) {
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndActiveTrue(notification.getRecipientId());
        if (tokens.isEmpty()) return;

        List<Map<String, Object>> messages = tokens.stream()
                .map(token -> buildExpoMessage(token.getExpoPushToken(), notification))
                .toList();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            restTemplate.exchange(
                    EXPO_PUSH_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(messages, headers),
                    String.class
            );
        } catch (Exception e) {
            log.warn("Failed to send Expo push notification for notificationId={}: {}",
                    notification.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildExpoMessage(String token, InAppNotification notification) {
        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType());
        data.put("referenceId", notification.getReferenceId());
        data.put("senderId", notification.getSenderId());

        Map<String, Object> message = new HashMap<>();
        message.put("to", token);
        message.put("title", resolveTitle(notification.getType()));
        message.put("body", notification.getContent());
        message.put("sound", "default");
        message.put("priority", "high");
        message.put("channelId", "riff-notifications");
        message.put("data", data);
        return message;
    }

    private String resolveTitle(String type) {
        String normalized = normalize(type);
        if (normalized == null) return "Riff";
        String lower = normalized.toLowerCase();
        if (lower.contains("friend") || lower.contains("relationship")) return "Lời mời kết bạn";
        if (lower.contains("call")) return "Cuộc gọi";
        if (lower.contains("group")) return "Nhóm";
        if (lower.contains("message")) return "Tin nhắn mới";
        return "Riff";
    }

    private boolean isExpoPushToken(String token) {
        return token != null
                && (token.startsWith("ExpoPushToken[") || token.startsWith("ExponentPushToken["));
    }

    private String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
