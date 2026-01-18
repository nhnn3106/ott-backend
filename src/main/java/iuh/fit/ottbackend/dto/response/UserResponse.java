package iuh.fit.ottbackend.dto.response;

import iuh.fit.ottbackend.entity.enums.AccountType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String phone;
    private String email;
    private String fullName;
    private String avatarUrl;
    private AccountType accountType;
    private Boolean isPhoneVerified;
    private Boolean isEmailVerified;
    private Boolean hasPassword;
    private LocalDateTime createdAt;
}