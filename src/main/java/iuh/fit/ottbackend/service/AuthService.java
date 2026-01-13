package iuh.fit.ottbackend.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.*;
import iuh.fit.ottbackend.entity.*;
import iuh.fit.ottbackend.entity.enums.*;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import iuh.fit.ottbackend.mapper.QrCodeMapper;
import iuh.fit.ottbackend.repository.*;
import iuh.fit.ottbackend.repository.httpclient.GoogleIdentityClient;
import iuh.fit.ottbackend.repository.httpclient.GoogleUserClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    @NonFinal
    @Value("${jwt.secret}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.expiration}")
    protected long EXPIRATION;

    @NonFinal
    @Value("${jwt.refresh-expiration}")
    protected long REFRESH_EXPIRATION;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    protected String CLIENT_ID;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    protected String CLIENT_SECRET;

    @NonFinal
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    protected String REDIRECT_URI;

    @NonFinal
    protected String GRANT_TYPE = "authorization_code";

    private static final int QR_EXPIRY_MINUTES = 3;
    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final GoogleIdentityClient googleIdentityClient;
    private final GoogleUserClient googleUserClient;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final UserSessionRepository userSessionRepository;
    private final QrCodeRepository qrCodeRepository;
    private final QrLoginSessionRepository qrLoginSessionRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);

    private final QrCodeMapper qrCodeMapper;

    /**
     * Introspect token để verify
     */
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        String token = request.getToken();
        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .valid(isValid)
                .build();
    }

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(REFRESH_EXPIRATION, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        var verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date())))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    /**
     * Đăng nhập bằng phone/password (LOCAL_PASSWORD)
     */
    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = userRepository
                .findByPhone(request.getPhone())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Check if user is active
        if (!user.getIsActive()) {
            logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Account not active");
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        // Check if user is blocked
        if (user.getIsBlocked()) {
            if (user.getBlockedUntil() != null && user.getBlockedUntil().isAfter(LocalDateTime.now())) {
                logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                        LoginStatus.FAILED, LoginMethod.LOCAL, "Account blocked");
                throw new AppException(ErrorCode.USER_BLOCKED);
            }
            // Unblock if time has passed
            user.setIsBlocked(false);
            user.setBlockedUntil(null);
            user.setBlockedReason(null);
            userRepository.save(user);
        }

        // Verify password
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!authenticated) {
            logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                    LoginStatus.FAILED, LoginMethod.LOCAL, "Invalid password");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        var token = generateToken(user);
        var refreshToken = generateSecureToken();

        // Create session and log login
        createUserSession(user, request.getDeviceId(), request.getDeviceType(),
                request.getDeviceName(), request.getIpAddress(), request.getDeviceInfo(),
                token, refreshToken, LoginMethod.LOCAL);

        logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                LoginStatus.SUCCESS, LoginMethod.LOCAL, null);

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Đăng nhập bằng Google OAuth2 (GOOGLE)
     */
    @Transactional
    public AuthenticationResponse googleAuthenticate(GoogleAuthRequest request) {
        var response = googleIdentityClient.exchangeToken(ExchangeTokenRequest.builder()
                .code(request.getCode())
                .clientId(CLIENT_ID)
                .clientSecret(CLIENT_SECRET)
                .redirectUri(REDIRECT_URI)
                .grantType(GRANT_TYPE)
                .build());

        log.info("Google TOKEN RESPONSE received");

        var userInfo = googleUserClient.getUserInfo("json", response.getAccessToken());
        log.info("Google User info: {}", userInfo.getEmail());

        // Tìm user theo googleId hoặc email
        User user = userRepository.findByGoogleId(userInfo.getId())
                .or(() -> userRepository.findByEmail(userInfo.getEmail()))
                .orElseGet(() -> {
                    // Nếu chưa có - cần verify phone trước khi tạo account
                    // Trả về error yêu cầu link phone number
                    throw new AppException(ErrorCode.PHONE_REQUIRED_FOR_GOOGLE);
                });

        // Nếu tìm thấy user nhưng chưa link Google ID
        if (user.getGoogleId() == null) {
            user.setGoogleId(userInfo.getId());
            user.setEmail(userInfo.getEmail());
            user.setIsEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());

            // Update avatar if not set
            if (user.getAvatarUrl() == null && userInfo.getPicture() != null) {
                user.setAvatarUrl(userInfo.getPicture());
            }

            userRepository.save(user);
        }

        // Check if user is active
        if (!user.getIsActive() || user.getIsBlocked()) {
            logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                    LoginStatus.FAILED, LoginMethod.GOOGLE, "Account not active or blocked");
            throw new AppException(ErrorCode.USER_NOT_ACTIVE);
        }

        var token = generateToken(user);
        var refreshToken = generateSecureToken();

        // Create session and log login
        createUserSession(user, request.getDeviceId(), request.getDeviceType(),
                request.getDeviceName(), request.getIpAddress(), request.getDeviceInfo(),
                token, refreshToken, LoginMethod.GOOGLE);

        logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                LoginStatus.SUCCESS, LoginMethod.GOOGLE, null);

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Link Google account với phone number (cho lần đầu đăng nhập Google)
     */
    @Transactional
    public AuthenticationResponse linkGoogleWithPhone(LinkGoogleRequest request) {
        // Verify OTP trước (implement trong OtpService)

        // Get Google user info
        var userInfo = googleUserClient.getUserInfo("json", request.getGoogleAccessToken());

        // Tìm user theo phone
        User user = userRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    // Tạo user mới
                    return User.builder()
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
                });

        // Link Google nếu user đã tồn tại
        if (user.getId() != null) {
            user.setGoogleId(userInfo.getId());
            user.setEmail(userInfo.getEmail());
            user.setIsEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
        }

        user = userRepository.save(user);

        var token = generateToken(user);
        var refreshToken = generateSecureToken();

        // Create session
        createUserSession(user, request.getDeviceId(), request.getDeviceType(),
                request.getDeviceName(), request.getIpAddress(), request.getDeviceInfo(),
                token, refreshToken, LoginMethod.GOOGLE);

        logLoginHistory(user, request.getIpAddress(), request.getDeviceInfo(),
                LoginStatus.SUCCESS, LoginMethod.GOOGLE, null);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Logout - invalidate token
     */
    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();
            String userId = signToken.getJWTClaimsSet().getStringClaim("userId");

            // Find user
            var user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            // Invalidate token
            InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                    .id(jit)
                    .expiryTime(LocalDateTime.ofInstant(expiryTime.toInstant(),
                            java.time.ZoneId.systemDefault()))
                    .user(user)
                    .tokenType("ACCESS")
                    .invalidatedAt(LocalDateTime.now())
                    .reason("User logout")
                    .build();

            invalidatedTokenRepository.save(invalidatedToken);

            // Revoke session if deviceId provided
            if (request.getDeviceId() != null) {
                userSessionRepository.findByDeviceIdAndUser(request.getDeviceId(), user)
                        .ifPresent(session -> {
                            session.setIsActive(false);
                            session.setRevokedAt(LocalDateTime.now());
                            session.setRevokedReason("User logout");
                            userSessionRepository.save(session);
                        });
            }

            log.info("User {} logged out successfully from device {}", userId, request.getDeviceId());

        } catch (AppException exception) {
            log.info("Token already expired during logout");
        }
    }

    /**
     * Refresh token
     */
    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signJWT = verifyToken(request.getToken(), true);

        var jit = signJWT.getJWTClaimsSet().getJWTID();
        var expiryTime = signJWT.getJWTClaimsSet().getExpirationTime();
        var userId = signJWT.getJWTClaimsSet().getStringClaim("userId");

        // Invalidate old token
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jit)
                .expiryTime(LocalDateTime.ofInstant(expiryTime.toInstant(),
                        java.time.ZoneId.systemDefault()))
                .tokenType("REFRESH")
                .invalidatedAt(LocalDateTime.now())
                .reason("Token refreshed")
                .build();

        invalidatedTokenRepository.save(invalidatedToken);

        // Get user and generate new token
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);
        var refreshToken = generateSecureToken();

        // Update session with new tokens
        if (request.getDeviceId() != null) {
            userSessionRepository.findByDeviceIdAndUser(request.getDeviceId(), user)
                    .ifPresent(session -> {
                        session.setSessionToken(token);
                        session.setRefreshToken(refreshToken);
                        session.setExpiresAt(LocalDateTime.now().plusSeconds(EXPIRATION));
                        session.setRefreshExpiresAt(LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION));
                        session.setLastActiveAt(LocalDateTime.now());
                        userSessionRepository.save(session);
                    });
        }

        return AuthenticationResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    /**
     * Generate QR code cho login (QR_CODE)
     * Bước 1: Web/Desktop tạo QR code
     */
    @Transactional
    public QrCodeResponse generateLoginQrCode(QrGenerateRequest request) {
        String secretToken = generateSecureToken();
        String qrId = UUID.randomUUID().toString();
        String qrData = qrId + ":" + secretToken;

        QrCode qrCode = QrCode.builder()
                .id(qrId)
                .qrType(QrCodeType.LOGIN)
                .qrData(Base64.getEncoder().encodeToString(qrData.getBytes()))
                .deviceId(request.getDeviceId())
                .deviceType(request.getDeviceType())
                .deviceInfo(request.getDeviceInfo())
                .ipAddress(request.getIpAddress())
                .status(QrCodeStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(QR_EXPIRY_MINUTES))
                .failedAttempts(0)
                .build();

        qrCode = qrCodeRepository.save(qrCode);

        log.info("Generated QR code {} for device {}", qrId, request.getDeviceId());

        return qrCodeMapper.toQrCodeResponse(qrCode);
    }

    /**
     * Bước 2: Mobile app quét QR code

     */
    @Transactional
    public QrStatusResponse scanQrCode(QrScanRequest request, String userId) {
        QrCode qrCode = verifyQrCode(request.getQrData());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));


        qrCode.setUser(user);
        qrCode.setStatus(QrCodeStatus.SCANNED);
        qrCode.setScannedAt(LocalDateTime.now());
        qrCode.setScannedDeviceId(request.getDeviceId());
        qrCode.setScannedDeviceType(request.getDeviceType());
        qrCode.setScannedDeviceInfo(request.getDeviceInfo());
        qrCode.setScannedIpAddress(request.getIpAddress());
        qrCode.setLocation(request.getLocation());
        qrCode = qrCodeRepository.save(qrCode);

        // Create QR login session
        QrLoginSession loginSession = QrLoginSession.builder()
                .qrCode(qrCode)
                .user(user)
                .status(QrLoginSessionStatus.WAITING)
                .build();
        qrLoginSessionRepository.save(loginSession);

        log.info("QR code {} scanned by user {}", qrCode.getId(), userId);


        QrStatusResponse response = qrCodeMapper.toQrStatusResponse(qrCode);
        response.setMessage("QR code scanned successfully. Please confirm to login.");
        return response;
    }

    /**
     * Bước 3: User xác nhận đăng nhập

     */
    @Transactional
    public QrStatusResponse confirmQrLogin(QrConfirmRequest request, String userId) {
        QrCode qrCode = qrCodeRepository.findById(request.getQrId())
                .orElseThrow(() -> new AppException(ErrorCode.QR_CODE_NOT_FOUND));

        if (qrCode.getStatus() != QrCodeStatus.SCANNED) {
            throw new AppException(ErrorCode.INVALID_QR_STATUS);
        }

        if (!userId.equals(qrCode.getUser().getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        QrLoginSession loginSession = qrLoginSessionRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

        if (!request.isConfirmed()) {
            qrCode.setStatus(QrCodeStatus.CANCELLED);
            qrCode = qrCodeRepository.save(qrCode);

            loginSession.setStatus(QrLoginSessionStatus.REJECTED);
            loginSession.setRejectedAt(LocalDateTime.now());
            loginSession.setRejectionReason("User rejected login");
            qrLoginSessionRepository.save(loginSession);

            QrStatusResponse response = qrCodeMapper.toQrStatusResponse(qrCode);
            response.setMessage("Login request cancelled");
            return response;
        }

        // User confirmed - create session
        User user = qrCode.getUser();
        String token = generateToken(user);
        String refreshToken = generateSecureToken();

        UserSession session = createUserSession(
                user,
                qrCode.getDeviceId(),
                qrCode.getDeviceType(),
                null,
                qrCode.getIpAddress(),
                qrCode.getDeviceInfo(),
                token,
                refreshToken,
                LoginMethod.QR_CODE
        );

        // Update QR code
        qrCode.setStatus(QrCodeStatus.CONFIRMED);
        qrCode.setConfirmedAt(LocalDateTime.now());
        qrCode = qrCodeRepository.save(qrCode);

        // Update login session
        loginSession.setStatus(QrLoginSessionStatus.AUTHORIZED);
        loginSession.setSession(session);
        loginSession.setAuthorizedAt(LocalDateTime.now());
        qrLoginSessionRepository.save(loginSession);

        // Log login history
        logLoginHistory(user, qrCode.getIpAddress(), qrCode.getDeviceInfo(),
                LoginStatus.SUCCESS, LoginMethod.QR_CODE, qrCode.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("QR login confirmed for user {} on device {}", userId, qrCode.getDeviceId());

        QrStatusResponse response = qrCodeMapper.toQrStatusResponse(qrCode);
        response.setSessionToken(token);
        response.setRefreshToken(refreshToken);
        response.setExpiresAt(session.getExpiresAt());
        response.setMessage("Login successful");
        return response;
    }

    /**
     * Bước 4: Web polling kiểm tra status
     */
    public QrStatusResponse checkQrStatus(String qrId) {
        QrCode qrCode = qrCodeRepository.findById(qrId)
                .orElseThrow(() -> new AppException(ErrorCode.QR_CODE_NOT_FOUND));

        // Check expiry
        if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            if (qrCode.getStatus() != QrCodeStatus.EXPIRED) {
                qrCode.setStatus(QrCodeStatus.EXPIRED);
                qrCode = qrCodeRepository.save(qrCode);
            }
        }

        QrStatusResponse response = qrCodeMapper.toQrStatusResponse(qrCode);

        // Set message và tokens dựa vào status
        if (qrCode.getStatus() == QrCodeStatus.CONFIRMED) {
            UserSession session = userSessionRepository
                    .findByDeviceIdAndUserAndIsActive(qrCode.getDeviceId(), qrCode.getUser(), true)
                    .orElseThrow(() -> new AppException(ErrorCode.SESSION_NOT_FOUND));

            response.setSessionToken(session.getSessionToken());
            response.setRefreshToken(session.getRefreshToken());
            response.setExpiresAt(session.getExpiresAt());
            response.setMessage("Login successful");
        } else if (qrCode.getStatus() == QrCodeStatus.SCANNED) {
            response.setMessage("QR code scanned. Waiting for confirmation...");
        } else if (qrCode.getStatus() == QrCodeStatus.EXPIRED) {
            response.setMessage("QR code expired. Please generate a new one.");
        } else if (qrCode.getStatus() == QrCodeStatus.CANCELLED) {
            response.setMessage("Login request was cancelled.");
        } else {
            response.setMessage("Waiting for QR code to be scanned...");
        }

        return response;
    }

    /**
     * Cancel QR code
     */
    @Transactional
    public void cancelQrCode(String qrId) {
        QrCode qrCode = qrCodeRepository.findById(qrId)
                .orElseThrow(() -> new AppException(ErrorCode.QR_CODE_NOT_FOUND));

        qrCode.setStatus(QrCodeStatus.CANCELLED);
        qrCodeRepository.save(qrCode);

        log.info("QR code {} cancelled", qrId);
    }

    // ==================== HELPER METHODS ====================

    private void logLoginHistory(User user, String ipAddress, String userAgent,
                                 LoginStatus status, LoginMethod loginMethod, String additionalInfo) {
        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .ipAddress(ipAddress)
                .deviceType(extractDeviceType(userAgent))
                .userAgent(userAgent)
                .status(status)
                .loginMethod(loginMethod)
                .qrCodeId(loginMethod == LoginMethod.QR_CODE ? additionalInfo : null)
                .failureReason(status == LoginStatus.FAILED ? additionalInfo : null)
                .build();

        loginHistoryRepository.save(loginHistory);
    }

    private DeviceType extractDeviceType(String deviceInfo) {
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

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        String scope = buildScope(user.getAccountType());

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getPhone())
                .issuer("ottbackend.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(EXPIRATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId())
                .claim("accountType", user.getAccountType().name())
                .claim("scope", scope)
                .claim("phone", user.getPhone())
                .claim("email", user.getEmail())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot generate token", e);
            throw new RuntimeException(e);
        }
    }

    private String buildScope(AccountType accountType) {
        switch (accountType) {
            case ADMIN:
                return "ADMIN";
            case OA:
                return "OA";
            case USER:
            default:
                return "USER";
        }
    }

    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserSession createUserSession(User user, String deviceId, DeviceType deviceType,
                                          String deviceName, String ipAddress, String userAgent,
                                          String sessionToken, String refreshToken, LoginMethod loginMethod) {
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
                .expiresAt(LocalDateTime.now().plusSeconds(EXPIRATION))
                .refreshExpiresAt(LocalDateTime.now().plusSeconds(REFRESH_EXPIRATION))
                .build();

        return userSessionRepository.save(session);
    }

    private QrCode verifyQrCode(String qrData) {
        try {
            String decodedData = new String(Base64.getDecoder().decode(qrData));
            String[] parts = decodedData.split(":");

            if (parts.length != 2) {
                throw new AppException(ErrorCode.INVALID_QR_CODE);
            }

            String qrId = parts[0];
            String secretToken = parts[1];

            QrCode qrCode = qrCodeRepository.findById(qrId)
                    .orElseThrow(() -> new AppException(ErrorCode.QR_CODE_NOT_FOUND));

            // Verify secret token
            String storedData = new String(Base64.getDecoder().decode(qrCode.getQrData()));
            if (!storedData.equals(decodedData)) {
                qrCode.setFailedAttempts(qrCode.getFailedAttempts() + 1);

                if (qrCode.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                    qrCode.setStatus(QrCodeStatus.EXPIRED);
                }

                qrCodeRepository.save(qrCode);
                throw new AppException(ErrorCode.INVALID_QR_CODE);
            }

            // Check expiry
            if (qrCode.getExpiresAt().isBefore(LocalDateTime.now())) {
                qrCode.setStatus(QrCodeStatus.EXPIRED);
                qrCodeRepository.save(qrCode);
                throw new AppException(ErrorCode.QR_CODE_EXPIRED);
            }

            // Check status
            if (qrCode.getStatus() != QrCodeStatus.PENDING) {
                throw new AppException(ErrorCode.QR_CODE_ALREADY_USED);
            }

            return qrCode;

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_QR_CODE);
        }
    }
}