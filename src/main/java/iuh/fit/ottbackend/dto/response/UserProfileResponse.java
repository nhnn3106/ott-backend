package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.AccountType;
import iuh.fit.ottbackend.entity.enums.Gender;
import lombok.*;

import java.time.LocalDate;
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
    private LocalDate dateOfBirth;
    private Gender gender;
    private AccountType accountType;
    private Boolean isPhoneVerified;
    private Boolean isEmailVerified;
    private Boolean hasPassword;
    private Boolean hasGoogleLinked;
    private LocalDateTime phoneVerifiedAt;
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}