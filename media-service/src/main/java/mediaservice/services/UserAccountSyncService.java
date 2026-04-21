package mediaservice.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mediaservice.dtos.external.UserServiceApiResponse;
import mediaservice.dtos.external.UserServiceUserResponse;
import mediaservice.models.UserAccount;
import mediaservice.repositories.AccountRepository;
import mediaservice.repositories.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAccountSyncService {

    private final RestTemplate restTemplate;
    private final UserAccountRepository userAccountRepository;
    private final AccountRepository accountRepository;

    @Value("${services.user.url}")
    private String userServiceUrl;

    @Value("${internal.api.key}")
    private String internalApiKey;

    public UserAccount syncUserAccountById(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        if (userAccountRepository.existsById(userId)) {
            return userAccountRepository.findById(userId).orElse(null);
        }

        try {
            String url = userServiceUrl + "/internal/users/" + userId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", internalApiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<UserServiceApiResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    UserServiceApiResponse.class
            );

            UserServiceApiResponse body = response.getBody();
            if (body == null || body.getResult() == null) {
                return null;
            }

            UserServiceUserResponse user = body.getResult();
            UserAccount userAccount = new UserAccount();
            userAccount.setId(user.getId());
            userAccount.setEmail(user.getEmail());
            userAccount.setAvatarUrl(user.getAvatarUrl());
            userAccount.setCoverUrl(user.getCoverUrl());

            String username = resolveUsername(user, userId);
            userAccount.setUsername(username);

            String displayName = resolveDisplayName(user, username);
            userAccount.setDisplayName(displayName);

            UserAccount saved = userAccountRepository.save(userAccount);
            log.info("[UserSync] Created user account for userId={}", userId);
            return saved;
        } catch (Exception ex) {
            log.warn("[UserSync] Failed to sync user account for userId={}", userId, ex);
            return null;
        }
    }

    private String resolveUsername(UserServiceUserResponse user, String userId) {
        String base = null;
        if (user.getEmail() != null && user.getEmail().contains("@")) {
            base = user.getEmail().substring(0, user.getEmail().indexOf('@'));
        } else if (user.getFullName() != null && !user.getFullName().isBlank()) {
            base = user.getFullName();
        }

        if (base == null || base.isBlank()) {
            base = "user";
        }

        String candidate = base.trim().replace(" ", "").toLowerCase();
        if (!accountRepository.existsByUsername(candidate)) {
            return candidate;
        }

        String suffix = userId.length() >= 6 ? userId.substring(0, 6) : userId;
        String withSuffix = candidate + "-" + suffix;
        if (!accountRepository.existsByUsername(withSuffix)) {
            return withSuffix;
        }

        int counter = 1;
        String fallback = withSuffix + "-" + counter;
        while (accountRepository.existsByUsername(fallback) && counter < 5) {
            counter++;
            fallback = withSuffix + "-" + counter;
        }
        return fallback;
    }

    private String resolveDisplayName(UserServiceUserResponse user, String username) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return username;
    }
}
