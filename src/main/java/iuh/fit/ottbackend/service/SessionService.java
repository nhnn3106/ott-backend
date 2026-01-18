package iuh.fit.ottbackend.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import iuh.fit.ottbackend.dto.response.SessionInfo;
import iuh.fit.ottbackend.dto.response.UserSessionsResponse;
import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.UserSession;
import iuh.fit.ottbackend.entity.enums.DeviceType;
import iuh.fit.ottbackend.entity.enums.LoginMethod;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.repository.UserSessionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final JwtService jwtService;
    @Transactional
    public UserSession createUserSession(User user, String deviceId, DeviceType deviceType,
                                         String deviceName, String ipAddress, String userAgent,
                                         String sessionToken, String refreshToken, LoginMethod loginMethod) {

        List<UserSession> existingSessions = userSessionRepository
                .findAllByDeviceIdAndUserAndIsActive(deviceId, user, true);

        if (!existingSessions.isEmpty()) {
            existingSessions.forEach(existingSession -> {
                existingSession.setIsActive(false);
                existingSession.setRevokedAt(LocalDateTime.now());
                existingSession.setRevokedReason("New login from same device");
            });
            userSessionRepository.saveAll(existingSessions);

            log.info("Revoked {} existing sessions for device {}", existingSessions.size(), deviceId);
        }

        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(sessionToken)
                .refreshToken(refreshToken)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .loginMethod(loginMethod)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpiration()))
                .refreshExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration()))
                .build();

        session = userSessionRepository.save(session);
        log.info("Created new session for user {} on device {}", user.getId(), deviceId);

        return session;
    }

    public UserSessionsResponse getUserSessions(String userId) {
        List<UserSession> sessions = userSessionRepository
                .findByUserIdAndIsActiveTrueOrderByLastActiveAtDesc(userId);

        List<SessionInfo> sessionInfos = sessions.stream()
                .map(this::toSessionInfo)
                .collect(Collectors.toList());

        return UserSessionsResponse.builder()
                .sessions(sessionInfos)
                .total(sessionInfos.size())
                .build();
    }

    @Transactional
    public void revokeSession(String userId, String sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!session.getUser().getId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!session.getIsActive()) {
            log.warn("Attempting to revoke already inactive session: {}", sessionId);
            return;
        }

        session.setIsActive(false);
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokedReason("Revoked by user");
        userSessionRepository.save(session);

        invalidateSessionTokens(session);

        log.info("Session revoked: {}", sessionId);
    }

    @Transactional
    public void revokeAllOtherSessions(String userId, String currentSessionToken) {
        List<UserSession> sessions = userSessionRepository.findByUserIdAndIsActiveTrue(userId);

        int revokedCount = 0;
        for (UserSession session : sessions) {
            if (!session.getSessionToken().equals(currentSessionToken)) {
                session.setIsActive(false);
                session.setRevokedAt(LocalDateTime.now());
                session.setRevokedReason("Revoked by user - all other sessions");

                invalidateSessionTokens(session);
                revokedCount++;
            }
        }

        if (revokedCount > 0) {
            userSessionRepository.saveAll(sessions);
            log.info("Revoked {} other sessions for user: {}", revokedCount, userId);
        }
    }

    @Transactional
    public void revokeAllUserSessions(String userId, String reason) {
        List<UserSession> sessions = userSessionRepository.findByUserIdAndIsActiveTrue(userId);

        if (sessions.isEmpty()) {
            log.info("No active sessions to revoke for user: {}", userId);
            return;
        }

        sessions.forEach(session -> {
            session.setIsActive(false);
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedReason(reason);

            invalidateSessionTokens(session);
        });

        userSessionRepository.saveAll(sessions);

        log.info("Revoked {} sessions for user: {} - reason: {}", sessions.size(), userId, reason);
    }

    public UserSession findActiveSessionByDeviceAndUser(String deviceId, User user) {
        return userSessionRepository
                .findByDeviceIdAndUserAndIsActive(deviceId, user, true)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));
    }

    @Transactional
    public void updateSessionTokens(String deviceId, User user, String newToken, String newRefreshToken) {
        userSessionRepository.findByDeviceIdAndUser(deviceId, user)
                .ifPresent(session -> {
                    if (!session.getIsActive()) {
                        log.warn("Attempting to update tokens for inactive session: {}", session.getId());
                        return;
                    }

                    session.setSessionToken(newToken);
                    session.setRefreshToken(newRefreshToken);
                    session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpiration()));
                    session.setRefreshExpiresAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration()));
                    session.setLastActiveAt(LocalDateTime.now());
                    userSessionRepository.save(session);

                    log.info("Updated session tokens for device {} and user {}", deviceId, user.getId());
                });
    }

    private void invalidateSessionTokens(UserSession session) {
        try {
            User user = session.getUser();

            if (session.getSessionToken() != null) {
                try {
                    SignedJWT signedJWT = SignedJWT.parse(session.getSessionToken());
                    String jwtId = signedJWT.getJWTClaimsSet().getJWTID();

                    jwtService.invalidateToken(
                            jwtId,
                            session.getExpiresAt(),
                            user,
                            "ACCESS",
                            "Session revoked"
                    );

                    log.debug("Invalidated access token for session: {}", session.getId());
                } catch (ParseException e) {
                    log.warn("Could not parse/invalidate access token for session {}: {}",
                            session.getId(), e.getMessage());
                }
            }

            if (session.getRefreshToken() != null) {
                String refreshTokenId = "refresh_" + session.getId();

                jwtService.invalidateToken(
                        refreshTokenId,
                        session.getRefreshExpiresAt(),
                        user,
                        "REFRESH",
                        "Session revoked"
                );

                log.debug("Invalidated refresh token for session: {}", session.getId());
            }

        } catch (Exception e) {
            log.error("Error invalidating session tokens for session {}: {}",
                    session.getId(), e.getMessage());
        }
    }

    private SessionInfo toSessionInfo(UserSession session) {
        return SessionInfo.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .deviceType(session.getDeviceType())
                .deviceName(session.getDeviceName())
                .ipAddress(session.getIpAddress())
                .loginMethod(session.getLoginMethod())
                .createdAt(session.getCreatedAt())
                .lastActiveAt(session.getLastActiveAt())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .build();
    }
}