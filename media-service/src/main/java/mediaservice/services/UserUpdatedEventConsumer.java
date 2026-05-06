package mediaservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mediaservice.dtos.events.UserUpdatedEvent;
import mediaservice.models.UserAccount;
import mediaservice.repositories.UserAccountRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdatedEventConsumer {

    private final UserAccountRepository userAccountRepository;

    @RabbitListener(queues = "media_service_user_updated")
    public void handleUserUpdated(UserUpdatedEvent event) {
        if (event == null || event.getUserId() == null || event.getUserId().isBlank()) {
            log.warn("[UserUpdated] Invalid event: {}", event);
            return;
        }

        Optional<UserAccount> userOpt = userAccountRepository.findById(event.getUserId());
        if (userOpt.isEmpty()) {
            log.warn("[UserUpdated] Account not found for userId={}. Ignoring update.", event.getUserId());
            return;
        }

        UserAccount user = userOpt.get();
        boolean isUpdated = false;

        if (event.getFullName() != null && !event.getFullName().isBlank() && !event.getFullName().equals(user.getDisplayName())) {
            user.setDisplayName(event.getFullName());
            isUpdated = true;
        }

        if (event.getAvatarUrl() != null && !event.getAvatarUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(event.getAvatarUrl());
            isUpdated = true;
        }

        if (event.getCoverUrl() != null && !event.getCoverUrl().equals(user.getCoverUrl())) {
            user.setCoverUrl(event.getCoverUrl());
            isUpdated = true;
        }

        if (event.getBio() != null && !event.getBio().equals(user.getBio())) {
            user.setBio(event.getBio());
            isUpdated = true;
        }

        if (isUpdated) {
            userAccountRepository.save(user);
            log.info("[UserUpdated] Updated user account for userId={}", event.getUserId());
        } else {
            log.debug("[UserUpdated] No relevant changes for userId={}", event.getUserId());
        }
    }
}
