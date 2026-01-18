package iuh.fit.ottbackend.service;

import com.nimbusds.jose.JOSEException;
import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.AuthenticationResponse;
import iuh.fit.ottbackend.dto.response.GoogleTokenResponse;
import iuh.fit.ottbackend.dto.response.OtpResponse;
import iuh.fit.ottbackend.entity.LoginHistory;
import iuh.fit.ottbackend.entity.OtpCode;
import iuh.fit.ottbackend.entity.User;
import iuh.fit.ottbackend.entity.enums.*;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.repository.LoginHistoryRepository;
import iuh.fit.ottbackend.repository.UserRepository;
import iuh.fit.ottbackend.repository.httpclient.GoogleUserClient;
import iuh.fit.ottbackend.utils.ValidationUtils;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {

    UserRepository userRepository;
    LoginHistoryRepository loginHistoryRepository;
    GoogleUserClient googleUserClient;
    PasswordEncoder passwordEncoder;
    OtpService otpService;
    EmailService emailService;
    JwtService jwtService;
    SessionService sessionService;
    ValidationUtils validationUtils;
    RestTemplate restTemplate;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    String CLIENT_ID;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    String CLIENT_SECRET;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    String REDIRECT_URI;

    private static final String GRANT_TYPE = "authorization_code";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        if (!validationUtils.isValidPhone(request.getPhone())) {
            throw new AppException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (user.getPasswordHash() == null) {
            logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Password not set");
            throw new AppException(ErrorCode.PASSWORD_NOT_SET);
        }

        validateUserStatus(user, request.getIpAddress(), request.getDeviceInfo());

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Invalid password");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return createAuthResponse(user, request, LoginMethod.LOCAL);
    }

    @Transactional
    public AuthenticationResponse googleAuthenticate(GoogleAuthRequest request) {
        String redirectUri = request.getRedirectUri() != null
                ? request.getRedirectUri()
                : REDIRECT_URI;

        log.info("========== GOOGLE AUTH DEBUG ==========");
        log.info("Code: {}", request.getCode().substring(0, Math.min(20, request.getCode().length())) + "...");
        log.info("Redirect URI: {}", redirectUri);
        log.info("Client ID: {}", CLIENT_ID);
        log.info("=======================================");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", request.getCode());
            params.add("client_id", CLIENT_ID);
            params.add("client_secret", CLIENT_SECRET);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", GRANT_TYPE);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(params, headers);

            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    GOOGLE_TOKEN_URL,
                    requestEntity,
                    GoogleTokenResponse.class
            );

            GoogleTokenResponse tokenResponse = response.getBody();

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.error("Google token response is null or missing access token");
                throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
            }

            log.info("Successfully exchanged code for Google access token");

            var userInfo = googleUserClient.getUserInfo("json", tokenResponse.getAccessToken());

            if (userInfo.getEmail() == null || !validationUtils.isValidEmail(userInfo.getEmail())) {
                throw new AppException(ErrorCode.INVALID_GOOGLE_EMAIL);
            }

            User user = userRepository.findByGoogleId(userInfo.getId()).orElse(null);

            if (user == null) {
                user = userRepository.findByEmail(userInfo.getEmail()).orElse(null);

                if (user == null) {
                    throw new AppException(ErrorCode.PHONE_REQUIRED_FOR_GOOGLE);
                }
                if (user.getGoogleId() != null && !user.getGoogleId().equals(userInfo.getId())) {
                    throw new AppException(ErrorCode.EMAIL_ALREADY_LINKED_TO_ANOTHER_GOOGLE);
                }

                user.setGoogleId(userInfo.getId());
                user.setEmail(userInfo.getEmail());
                user.setIsEmailVerified(true);
                user.setEmailVerifiedAt(LocalDateTime.now());

                if (user.getAvatarUrl() == null && userInfo.getPicture() != null) {
                    user.setAvatarUrl(userInfo.getPicture());
                }

                user = userRepository.save(user);
                log.info("Linked Google account {} to existing user {}", userInfo.getId(), user.getId());
            }

            validateUserStatus(user, request.getIpAddress(), request.getDeviceInfo());

            return createAuthResponse(user, request, LoginMethod.GOOGLE);

        } catch (Exception e) {
            log.error("Google authentication failed: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.GOOGLE_AUTH_FAILED);
        }
    }

    @Transactional
    public AuthenticationResponse linkGoogleWithPhone(LinkGoogleRequest request) {
        if (!validationUtils.isValidPhone(request.getPhone())) {
            throw new AppException(ErrorCode.INVALID_PHONE_FORMAT);
        }

        var userInfo = googleUserClient.getUserInfo("json", request.getGoogleAccessToken());

        if (userInfo.getEmail() == null || !validationUtils.isValidEmail(userInfo.getEmail())) {
            throw new AppException(ErrorCode.INVALID_GOOGLE_EMAIL);
        }

        userRepository.findByGoogleId(userInfo.getId()).ifPresent(existingUser -> {
            throw new AppException(ErrorCode.GOOGLE_ACCOUNT_ALREADY_LINKED);
        });

        User user = userRepository.findByPhone(request.getPhone()).orElse(null);

        if (user == null) {
            user = User.builder()
                    .phone(request.getPhone())
                    .email(userInfo.getEmail())
                    .googleId(userInfo.getId())
                    .fullName(userInfo.getName())
                    .avatarUrl(userInfo.getPicture())
                    .accountType(AccountType.USER)
                    .isPhoneVerified(true)
                    .phoneVerifiedAt(LocalDateTime.now())
                    .isEmailVerified(true)
                    .emailVerifiedAt(LocalDateTime.now())
                    .isActive(true)
                    .isBlocked(false)
                    .build();

            user = userRepository.save(user);
            log.info("Created new user with Google account and phone: {}", user.getId());
        } else {
            if (user.getGoogleId() != null) {
                if (user.getGoogleId().equals(userInfo.getId())) {
                    log.info("Google account already linked to this phone");
                } else {
                    throw new AppException(ErrorCode.GOOGLE_ALREADY_LINKED);
                }
            } else {
                user.setGoogleId(userInfo.getId());
                if (user.getEmail() == null) {
                    if (userRepository.existsByEmail(userInfo.getEmail())) {
                        throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
                    }
                    user.setEmail(userInfo.getEmail());
                    user.setIsEmailVerified(true);
                    user.setEmailVerifiedAt(LocalDateTime.now());
                } else if (!user.getEmail().equalsIgnoreCase(userInfo.getEmail())) {
                    log.warn("Google email {} differs from user email {} - keeping user email",
                            userInfo.getEmail(), user.getEmail());
                }

                if (user.getAvatarUrl() == null && userInfo.getPicture() != null) {
                    user.setAvatarUrl(userInfo.getPicture());
                }

                user = userRepository.save(user);
                log.info("Linked Google account {} to user {}", userInfo.getId(), user.getId());
            }
        }

        return createAuthResponse(user, request, LoginMethod.GOOGLE);
    }

    @Transactional
    public OtpResponse requestEmailOtp(EmailOtpRequest request) {
        if (!validationUtils.isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        OtpCode otp = otpService.generateOtp(
                null,
                request.getEmail(),
                OtpType.LOGIN_OTP_EMAIL,
                request.getIpAddress()
        );

        String fullName = userRepository.findByEmail(request.getEmail())
                .map(User::getFullName)
                .orElse("User");

        emailService.sendOtpEmail(
                request.getEmail(),
                fullName,
                otp.getCode(),
                OtpType.LOGIN_OTP_EMAIL,
                request.getIpAddress(),
                request.getLocation()
        );

        log.info("Login OTP sent to email: {}", validationUtils.maskEmail(request.getEmail()));

        return OtpResponse.builder()
                .message("OTP has been sent to your email")
                .email(validationUtils.maskEmail(request.getEmail()))
                .expiresAt(otp.getExpiresAt())
                .build();
    }
    @Transactional
    public AuthenticationResponse verifyEmailOtp(VerifyEmailOtpRequest request) {
        if (!validationUtils.isValidEmail(request.getEmail())) {
            throw new AppException(ErrorCode.INVALID_EMAIL_FORMAT);
        }

        OtpCode otp = otpService.validateOtp(
                null,
                request.getEmail(),
                request.getOtpCode(),
                OtpType.LOGIN_OTP_EMAIL
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        validateUserStatus(user, request.getIpAddress(), request.getDeviceInfo());

        if (!user.getIsEmailVerified()) {
            user.setIsEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        otpService.markOtpAsUsed(otp);

        return createAuthResponse(user, request, LoginMethod.OTP);
    }

    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = jwtService.verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            String userId = signToken.getJWTClaimsSet().getStringClaim("userId");

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            jwtService.invalidateToken(
                    jit,
                    LocalDateTime.ofInstant(expiryTime.toInstant(), java.time.ZoneId.systemDefault()),
                    user,
                    "ACCESS",
                    "User logout"
            );

            if (request.getDeviceId() != null) {
                sessionService.revokeSession(userId, request.getDeviceId());
            }

            log.info("User {} logged out successfully from device {}", userId, request.getDeviceId());

        } catch (AppException exception) {
            log.info("Token already expired during logout");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = jwtService.verifyToken(request.getToken(), true);

        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();
        var userId = signJWT.getJWTClaimsSet().getStringClaim("userId");

        jwtService.invalidateToken(
                jit,
                LocalDateTime.ofInstant(expiryTime.toInstant(), java.time.ZoneId.systemDefault()),
                null,
                "REFRESH",
                "Token refreshed"
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        if (!user.getIsActive() || user.getDeletedAt() != null) {
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

        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        if (request.getDeviceId() != null) {
            sessionService.updateSessionTokens(request.getDeviceId(), user, token, refreshToken);
        }

        log.info("Token refreshed for user: {}", userId);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    private void validateUserStatus(User user, String ipAddress, String deviceInfo) {
        if (user.getDeletedAt() != null) {
            logLoginHistory(user, ipAddress, deviceInfo,
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Account deleted");
            throw new AppException(ErrorCode.ACCOUNT_DELETED);
        }

        if (!user.getIsActive()) {
            logLoginHistory(user, ipAddress, deviceInfo,
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Account not active");
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        if (user.getIsBlocked()) {
            if (user.getBlockedUntil() != null && user.getBlockedUntil().isAfter(LocalDateTime.now())) {
                logLoginHistory(user, ipAddress, deviceInfo,
                        LoginStatus.FAILED, LoginMethod.LOCAL, "Account blocked");
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
            user.setIsBlocked(false);
            user.setBlockedUntil(null);
            user.setBlockedReason(null);
            userRepository.save(user);
        }
    }

    private AuthenticationResponse createAuthResponse(User user, Object request, LoginMethod loginMethod) {
        String token = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        String deviceId = extractField(request, "getDeviceId");
        DeviceType deviceType = extractDeviceType(request);
        String deviceName = extractField(request, "getDeviceName");
        String ipAddress = extractField(request, "getIpAddress");
        String deviceInfo = extractField(request, "getDeviceInfo");

        sessionService.createUserSession(
                user, deviceId, deviceType, deviceName,
                ipAddress, deviceInfo, token, refreshToken, loginMethod
        );

        logLoginHistory(user, ipAddress, deviceInfo, LoginStatus.SUCCESS, loginMethod, null);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    private void logLoginHistory(User user, String ipAddress, String userAgent,
                                 LoginStatus status, LoginMethod loginMethod, String additionalInfo) {
        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .ipAddress(ipAddress)
                .deviceType(extractDeviceTypeFromUserAgent(userAgent))
                .userAgent(userAgent)
                .status(status)
                .loginMethod(loginMethod)
                .qrCodeId(loginMethod == LoginMethod.QR_CODE ? additionalInfo : null)
                .failureReason(status == LoginStatus.FAILED ? additionalInfo : null)
                .build();

        loginHistoryRepository.save(loginHistory);
    }

    private DeviceType extractDeviceTypeFromUserAgent(String deviceInfo) {
        if (deviceInfo == null) return DeviceType.UNKNOWN;

        String lower = deviceInfo.toLowerCase();
        if (lower.contains("mobile") || lower.contains("android") || lower.contains("iphone")) {
            return DeviceType.MOBILE;
        } else if (lower.contains("tablet") || lower.contains("ipad")) {
            return DeviceType.TABLET;
        } else if (lower.contains("smart-tv") || lower.contains("tv")) {
            return DeviceType.TV;
        } else {
            return DeviceType.DESKTOP;
        }
    }

    private DeviceType extractDeviceType(Object request) {
        try {
            String deviceTypeStr = (String) request.getClass()
                    .getMethod("getDeviceType").invoke(request);
            return DeviceType.valueOf(deviceTypeStr.toUpperCase());
        } catch (Exception e) {
            return DeviceType.UNKNOWN;
        }
    }

    private String extractField(Object request, String methodName) {
        try {
            return (String) request.getClass().getMethod(methodName).invoke(request);
        } catch (Exception e) {
            return null;
        }
    }
}