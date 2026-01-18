package iuh.fit.ottbackend.service;

import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.OtpResponse;
import iuh.fit.ottbackend.entity.OtpCode;
import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.enums.OtpType;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.repository.UserRepository;
import iuh.fit.ottbackend.utils.ValidationUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SessionService sessionService;
    private final ValidationUtils validationUtils;

    @Transactional
    public void setPassword(String userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));


        validateUserStatus(user);

        if (user.getPasswordHash() != null) {
            throw new AppException(ErrorCode.PASSWORD_ALREADY_SET);
        }

        if (!validationUtils.isValidPassword(request.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        log.info("Password set for user: {}", userId);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateUserStatus(user);

        if (user.getPasswordHash() == null) {
            throw new AppException(ErrorCode.PASSWORD_NOT_SET);
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INCORRECT_PASSWORD);
        }

        if (!validationUtils.isValidPassword(request.getNewPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionService.revokeAllUserSessions(user.getId(), "Password changed");

        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public OtpResponse requestPasswordReset(ForgotPasswordRequest request) {
        if (request.getPhone() == null && request.getEmail() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getEmail() != null && !validationUtils.isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (request.getPhone() != null && !validationUtils.isValidPhone(request.getPhone())) {
            throw new AppException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        User user = findUserByPhoneOrEmail(request.getPhone(), request.getEmail());

        validateUserStatus(user);

        String emailToSend = determineEmailForOtp(request.getEmail(), user.getEmail());

        if (emailToSend == null) {
            throw new AppException(ErrorCode.EMAIL_REQUIRED_FOR_PASSWORD_RESET);
        }

        OtpCode otpCode = otpService.generateOtp(
                request.getPhone(),
                emailToSend,
                OtpType.RESET_PASSWORD,
                request.getIpAddress()
        );

        emailService.sendOtpEmail(
                emailToSend,
                user.getFullName(),
                otpCode.getCode(),
                OtpType.RESET_PASSWORD,
                request.getIpAddress(),
                null
        );

        log.info("Password reset OTP sent to email: {} for user: {}",
                validationUtils.maskEmail(emailToSend), user.getId());

        return OtpResponse.builder()
                .phone(request.getPhone())
                .email(validationUtils.maskEmail(emailToSend))
                .expiresAt(otpCode.getExpiresAt())
                .message("OTP has been sent to your email address")
                .build();
    }

    @Transactional
    public void verifyPasswordReset(VerifyPasswordResetRequest request) {
        if (request.getPhone() == null && request.getEmail() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (request.getEmail() != null && !validationUtils.isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        if (request.getPhone() != null && !validationUtils.isValidPhone(request.getPhone())) {
            throw new AppException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        User user = findUserByPhoneOrEmail(request.getPhone(), request.getEmail());

        validateUserStatus(user);

        OtpCode otpCode = otpService.validateOtp(
                request.getPhone(),
                request.getEmail(),
                request.getOtp(),
                OtpType.RESET_PASSWORD
        );

        if (!validationUtils.isValidPassword(request.getNewPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        otpService.markOtpAsUsed(otpCode);

        sessionService.revokeAllUserSessions(user.getId(), "Password reset");

        log.info("Password reset successfully for user: {}", user.getId());
    }

    @Transactional
    public void deleteAccount(String userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_DELETED);
        }

        if (user.getPasswordHash() != null) {
            if (request.getPassword() == null) {
                throw new AppException(ErrorCode.PASSWORD_REQUIRED);
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                throw new AppException(ErrorCode.INCORRECT_PASSWORD);
            }
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);

        sessionService.revokeAllUserSessions(userId, "Account deleted");

        log.info("Account soft deleted: {}", userId);
    }

    private User findUserByPhoneOrEmail(String phone, String email) {
        if (phone == null && email == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (phone != null) {
            return userRepository.findByPhone(phone)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String determineEmailForOtp(String requestEmail, String userEmail) {
        if (requestEmail != null) {
            if (userEmail != null && !requestEmail.equalsIgnoreCase(userEmail)) {
                throw new AppException(ErrorCode.EMAIL_MISMATCH);
            }
            return requestEmail;
        }

        return userEmail;
    }

    private void validateUserStatus(User user) {
        if (user.getDeletedAt() != null) {
            throw new AppException(ErrorCode.ACCOUNT_DELETED);
        }

        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (user.getIsBlocked()) {
            if (user.getBlockedUntil() != null && user.getBlockedUntil().isAfter(LocalDateTime.now())) {
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
            user.setIsBlocked(false);
            user.setBlockedUntil(null);
            user.setBlockedReason(null);
            userRepository.save(user);
        }
    }
}