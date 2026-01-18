package iuh.fit.ottbackend.service;

import iuh.fit.ottbackend.dto.request.RegisterRequest;
import iuh.fit.ottbackend.dto.response.UserResponse;
import iuh.fit.ottbackend.entity.OtpCode;
import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.enums.AccountType;
import iuh.fit.ottbackend.entity.enums.OtpType;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.mapper.UserMapper;
import iuh.fit.ottbackend.repository.UserRepository;
import iuh.fit.ottbackend.utils.ValidationUtils;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    UserMapper userMapper;
    OtpService otpService;
    ValidationUtils validationUtils;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!validationUtils.isValidPhone(request.getPhone())) {
            throw new AppException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        if (!validationUtils.isValidPassword(request.getPassword())) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new AppException(ErrorCode.FULL_NAME_REQUIRED);
        }

        String sanitizedName = validationUtils.sanitizeString(request.getFullName());
        if (sanitizedName.length() > 100) {
            throw new AppException(ErrorCode.INVALID_FULL_NAME);
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }

        OtpCode otpCode = otpService.validateOtp(
                request.getPhone(),
                null,
                request.getOtp(),
                OtpType.REGISTER
        );

        User user = User.builder()
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(sanitizedName)
                .accountType(AccountType.USER)
                .isPhoneVerified(true)
                .phoneVerifiedAt(LocalDateTime.now())
                .isActive(true)
                .isBlocked(false)
                .build();

        user = userRepository.save(user);

        otpService.markOtpAsUsed(otpCode);

        log.info("User registered successfully: {} with phone: {}",
                user.getId(), request.getPhone());

        return userMapper.toUserResponse(user);
    }
}