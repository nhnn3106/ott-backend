package iuh.fit.ottbackend.service;

import iuh.fit.ottbackend.entity.OtpCode;
import iuh.fit.ottbackend.entity.enums.OtpType;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.repository.OtpCodeRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class OtpService {

    final OtpCodeRepository otpCodeRepository;

    @Value("${otp.expiry-minutes}")
    int OTP_EXPIRY_MINUTES;

    @Value("${otp.length}")
    int OTP_LENGTH;

    @Value("${otp.max-attempts}")
    int MAX_OTP_ATTEMPTS;

    @Value("${otp.rate-limit-per-hour}")
    int RATE_LIMIT_PER_HOUR;

    @Transactional
    public OtpCode generateOtp(String phone, String email, OtpType type, String ipAddress) {
        if (phone == null && email == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (isEmailBasedOtpType(type) && email == null) {
            throw new AppException(ErrorCode.EMAIL_REQUIRED);
        }

        checkRateLimit(phone, email, type);

        invalidateOldOtps(phone, email, type);

        String code = generateRandomOtp(OTP_LENGTH);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        OtpCode otpCode = OtpCode.builder()
                .phone(phone)
                .email(email)
                .code(code)
                .type(type)
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .isUsed(false)
                .attempts(0)
                .build();

        otpCode = otpCodeRepository.save(otpCode);

        log.info("OTP generated - phone: {}, email: {}, type: {}, code: {}",
                phone, email, type, code);

        return otpCode;
    }

    @Transactional
    public OtpCode validateOtp(String phone, String email, String code, OtpType type) {
        if (phone == null && email == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (code == null || code.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_OTP_CODE);
        }

        if (!code.matches("\\d+")) {
            throw new AppException(ErrorCode.INVALID_OTP_CODE);
        }

        OtpCode otpCode = findLatestValidOtp(phone, email, type);

        if (otpCode.getIsUsed()) {
            throw new AppException(ErrorCode.OTP_ALREADY_USED);
        }

        if (otpCode.isExpired()) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (otpCode.getAttempts() >= MAX_OTP_ATTEMPTS) {
            throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
        }

        if (!otpCode.getCode().equals(code.trim())) {
            otpCode.setAttempts(otpCode.getAttempts() + 1);
            otpCodeRepository.save(otpCode);

            int remainingAttempts = MAX_OTP_ATTEMPTS - otpCode.getAttempts();

            if (remainingAttempts <= 0) {
                throw new AppException(ErrorCode.OTP_MAX_ATTEMPTS_EXCEEDED);
            }

            throw new AppException(
                    ErrorCode.INVALID_OTP_CODE,
                    "Invalid OTP. " + remainingAttempts + " attempts remaining."
            );
        }

        log.info("OTP validated successfully - phone: {}, email: {}, type: {}", phone, email, type);

        return otpCode;
    }

    @Transactional
    public void markOtpAsUsed(OtpCode otpCode) {
        if (otpCode.getIsUsed()) {
            log.warn("Attempting to mark already used OTP as used: {}", otpCode.getId());
            return;
        }

        otpCode.setIsUsed(true);
        otpCode.setUsedAt(LocalDateTime.now());
        otpCodeRepository.save(otpCode);

        log.info("OTP marked as used: {}", otpCode.getId());
    }

    @Transactional
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        List<OtpCode> expiredOtps = otpCodeRepository.findByExpiresAtBeforeAndIsUsedFalse(now);

        if (!expiredOtps.isEmpty()) {
            expiredOtps.forEach(otp -> {
                otp.setIsUsed(true);
                otp.setUsedAt(now);
            });
            otpCodeRepository.saveAll(expiredOtps);

            log.info("Cleaned up {} expired OTPs", expiredOtps.size());
        }
    }

    private OtpCode findLatestValidOtp(String phone, String email, OtpType type) {
        LocalDateTime now = LocalDateTime.now();
        List<OtpCode> otpCodes;

        if (phone != null) {
            otpCodes = otpCodeRepository
                    .findByPhoneAndTypeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            phone, type, now);
        } else if (email != null) {
            otpCodes = otpCodeRepository
                    .findByEmailAndTypeAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                            email, type, now);
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (otpCodes.isEmpty()) {
            throw new AppException(ErrorCode.OTP_NOT_FOUND);
        }

        return otpCodes.get(0);
    }

    private void checkRateLimit(String phone, String email, OtpType type) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count;

        if (phone != null) {
            count = otpCodeRepository.countRecentOtpByPhone(phone, type, oneHourAgo);
        } else if (email != null) {
            count = otpCodeRepository.countRecentOtpByEmail(email, type, oneHourAgo);
        } else {
            return;
        }

        if (count >= RATE_LIMIT_PER_HOUR) {
            log.warn("OTP rate limit exceeded - phone: {}, email: {}, type: {}, count: {}",
                    phone, email, type, count);
            throw new AppException(ErrorCode.OTP_RATE_LIMIT_EXCEEDED);
        }
    }

    private void invalidateOldOtps(String phone, String email, OtpType type) {
        List<OtpCode> oldOtps;

        if (phone != null) {
            oldOtps = otpCodeRepository.findByPhoneAndTypeAndIsUsedFalse(phone, type);
        } else if (email != null) {
            oldOtps = otpCodeRepository.findByEmailAndTypeAndIsUsedFalse(email, type);
        } else {
            return;
        }

        if (!oldOtps.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            oldOtps.forEach(otp -> {
                otp.setIsUsed(true);
                otp.setUsedAt(now);
            });
            otpCodeRepository.saveAll(oldOtps);
            log.info("Invalidated {} old OTPs for new request", oldOtps.size());
        }
    }

    private String generateRandomOtp(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();

        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }

        return otp.toString();
    }

    private boolean isEmailBasedOtpType(OtpType type) {
        return type == OtpType.LOGIN_OTP_EMAIL ||
                type == OtpType.EMAIL_VERIFICATION ||
                type == OtpType.RESET_PASSWORD ||
                type == OtpType.CHANGE_EMAIL;
    }
}