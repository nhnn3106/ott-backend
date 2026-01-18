package iuh.fit.ottbackend.controller;

import iuh.fit.ottbackend.dto.response.ApiResponse;
import iuh.fit.ottbackend.dto.response.UserSessionsResponse;
import iuh.fit.ottbackend.service.SessionService;
import iuh.fit.ottbackend.utils.ControllerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;
    private final ControllerUtils controllerUtils;

    @GetMapping
    public ApiResponse<UserSessionsResponse> getUserSessions() {
        String userId = controllerUtils.getCurrentUserId();
        UserSessionsResponse response = sessionService.getUserSessions(userId);

        return ApiResponse.<UserSessionsResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> revokeSession(@PathVariable String sessionId) {
        String userId = controllerUtils.getCurrentUserId();
        sessionService.revokeSession(userId, sessionId);

        log.info("Session {} revoked by user: {}", sessionId, userId);

        return ApiResponse.<Void>builder()
                .message("Session revoked successfully")
                .build();
    }

    @DeleteMapping("/others")
    public ApiResponse<Void> revokeAllOtherSessions() {
        String userId = controllerUtils.getCurrentUserId();
        String currentToken = controllerUtils.getCurrentSessionToken();

        sessionService.revokeAllOtherSessions(userId, currentToken);

        log.info("All other sessions revoked for user: {}", userId);

        return ApiResponse.<Void>builder()
                .message("All other sessions revoked successfully")
                .build();
    }
}