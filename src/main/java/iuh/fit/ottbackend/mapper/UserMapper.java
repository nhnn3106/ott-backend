package iuh.fit.ottbackend.mapper;

import iuh.fit.ottbackend.dto.response.SessionInfo;
import iuh.fit.ottbackend.dto.response.UserProfileResponse;
import iuh.fit.ottbackend.dto.response.UserResponse;
import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.UserSession;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .accountType(user.getAccountType())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isEmailVerified(user.getIsEmailVerified())
                .hasPassword(user.getPasswordHash() != null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserProfileResponse toUserProfileResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .bio(user.getBio())
                .dateOfBirth(user.getDateOfBirth())
                .gender(user.getGender())
                .accountType(user.getAccountType())
                .isPhoneVerified(user.getIsPhoneVerified())
                .isEmailVerified(user.getIsEmailVerified())
                .hasPassword(user.getPasswordHash() != null)
                .hasGoogleLinked(user.getGoogleId() != null)
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public SessionInfo toSessionInfo(UserSession session) {
        if (session == null) {
            return null;
        }

        return SessionInfo.builder()
                .id(session.getId())
                .deviceId(session.getDeviceId())
                .deviceType(session.getDeviceType())
                .deviceName(session.getDeviceName())
                .ipAddress(session.getIpAddress())
                .userAgent(session.getUserAgent())
                .loginMethod(session.getLoginMethod())
                .isActive(session.getIsActive())
                .isCurrent(false)
                .lastActiveAt(session.getLastActiveAt())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }
}