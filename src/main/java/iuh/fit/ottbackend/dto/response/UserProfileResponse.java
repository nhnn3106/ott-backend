package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.AccountType;
import iuh.fit.ottbackend.entity.enums.Gender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String coverUrl;
    private String bio;
    private Gender gender;
    private LocalDateTime dateOfBirth;
    private AccountType accountType;
    private Boolean isActive;
    private Boolean isBlocked;
    private String blockedReason;
    private LocalDateTime blockedUntil;
    private Boolean isEmailVerified;
    private Boolean isPhoneVerified;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime phoneVerifiedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
