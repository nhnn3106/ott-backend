package iuh.fit.ottbackend.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    // ==================== GENERAL ERRORS (1001-1019) ====================
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1002, "You do not have permission", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    VALIDATION_FAILED(1004, "Validation failed", HttpStatus.BAD_REQUEST),

    // ==================== USER ERRORS (1005-1029) ====================
    USER_EXISTED(1005, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1006, "User not found", HttpStatus.NOT_FOUND),
    USER_NOT_ACTIVE(1007, "User account is not active", HttpStatus.FORBIDDEN),
    USERNAME_INVALID(1008, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1009, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(1010, "Invalid email format", HttpStatus.BAD_REQUEST),
    INVALID_PHONE(1011, "Invalid phone number", HttpStatus.BAD_REQUEST),
    INVALID_DATE_OF_BIRTH(1012, "You must be at least 18 years old", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1013, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    PHONE_EXISTED(1014, "Phone number already exists", HttpStatus.BAD_REQUEST),
    EMAIL_EXISTED(1015, "Email already exists", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1016, "Invalid phone number or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1017, "Account is locked due to multiple failed login attempts", HttpStatus.FORBIDDEN),
    PASSWORD_EXPIRED(1018, "Password has expired", HttpStatus.UNAUTHORIZED),
    USER_BLOCKED(1019, "User account is blocked", HttpStatus.FORBIDDEN),

    // ==================== TOKEN ERRORS (1020-1029) ====================
    TOKEN_EXPIRED(1020, "Token has expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(1021, "Invalid token", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_EXPIRED(1022, "Refresh token has expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1023, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(1024, "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_NOT_FOUND(1025, "Token not found", HttpStatus.NOT_FOUND),
    INVALID_REFRESH_TOKEN(1026, "Invalid refresh token", HttpStatus.UNAUTHORIZED),

    // ==================== OTP ERRORS (1030-1039) ====================
    OTP_EXPIRED(1030, "OTP has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1031, "Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_ALREADY_USED(1032, "OTP has already been used", HttpStatus.BAD_REQUEST),
    OTP_NOT_FOUND(1033, "OTP not found", HttpStatus.NOT_FOUND),
    INVALID_OTP(1034, "Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_MAX_ATTEMPTS(1035, "Maximum OTP attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),
    OTP_SEND_FAILED(1036, "Failed to send OTP", HttpStatus.INTERNAL_SERVER_ERROR),
    OTP_TOO_MANY_REQUESTS(1037, "Too many OTP requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    OTP_VERIFICATION_REQUIRED(1038, "OTP verification required", HttpStatus.BAD_REQUEST),

    // ==================== QR CODE ERRORS (1040-1049) ====================
    QR_CODE_NOT_FOUND(1041, "QR code not found", HttpStatus.NOT_FOUND),
    INVALID_QR_CODE(1042, "Invalid QR code", HttpStatus.BAD_REQUEST),
    QR_CODE_EXPIRED(1043, "QR code has expired", HttpStatus.BAD_REQUEST),
    QR_CODE_ALREADY_USED(1044, "QR code has already been used", HttpStatus.BAD_REQUEST),
    INVALID_QR_STATUS(1045, "Invalid QR code status for this operation", HttpStatus.BAD_REQUEST),
    QR_CODE_CANCELLED(1046, "QR code has been cancelled", HttpStatus.BAD_REQUEST),
    QR_CODE_SCAN_FAILED(1047, "Failed to scan QR code", HttpStatus.BAD_REQUEST),
    QR_CODE_MAX_ATTEMPTS(1048, "Maximum QR code scan attempts exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // ==================== SESSION ERRORS (1050-1059) ====================
    SESSION_NOT_FOUND(1050, "Session not found", HttpStatus.NOT_FOUND),
    SESSION_EXPIRED(1051, "Session has expired", HttpStatus.UNAUTHORIZED),
    MAX_SESSIONS_EXCEEDED(1052, "Maximum number of sessions exceeded", HttpStatus.BAD_REQUEST),
    SESSION_REVOKED(1053, "Session has been revoked", HttpStatus.UNAUTHORIZED),
    INVALID_SESSION(1054, "Invalid session", HttpStatus.UNAUTHORIZED),
    SESSION_CONFLICT(1055, "Session conflict detected", HttpStatus.CONFLICT),

    // ==================== ROLE & PERMISSION ERRORS (1060-1069) ====================
    ROLE_NOT_FOUND(1060, "Role not found", HttpStatus.NOT_FOUND),
    PERMISSION_NOT_FOUND(1061, "Permission not found", HttpStatus.NOT_FOUND),
    ROLE_ALREADY_ASSIGNED(1062, "Role has already been assigned to user", HttpStatus.BAD_REQUEST),
    CANNOT_DELETE_SYSTEM_ROLE(1063, "Cannot delete system role", HttpStatus.FORBIDDEN),
    ROLE_IN_USE(1064, "Role is currently in use and cannot be deleted", HttpStatus.BAD_REQUEST),
    PERMISSION_ALREADY_EXISTS(1065, "Permission already exists", HttpStatus.BAD_REQUEST),

    // ==================== TWO FACTOR AUTH ERRORS (1070-1079) ====================
    TWO_FACTOR_NOT_ENABLED(1070, "Two-factor authentication is not enabled", HttpStatus.BAD_REQUEST),
    TWO_FACTOR_ALREADY_ENABLED(1071, "Two-factor authentication is already enabled", HttpStatus.BAD_REQUEST),
    INVALID_BACKUP_CODE(1072, "Invalid backup code", HttpStatus.BAD_REQUEST),
    INVALID_2FA_CODE(1073, "Invalid two-factor authentication code", HttpStatus.BAD_REQUEST),
    TWO_FACTOR_REQUIRED(1074, "Two-factor authentication is required", HttpStatus.UNAUTHORIZED),
    BACKUP_CODES_EXHAUSTED(1075, "All backup codes have been used", HttpStatus.BAD_REQUEST),

    // ==================== FILE ERRORS (1080-1089) ====================
    FILE_TOO_LARGE(1080, "File size exceeds maximum limit", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(1081, "Invalid file type", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(1082, "Failed to upload file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(1083, "File not found", HttpStatus.NOT_FOUND),
    FILE_DOWNLOAD_FAILED(1084, "Failed to download file", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_DELETE_FAILED(1085, "Failed to delete file", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_NAME(1086, "Invalid file name", HttpStatus.BAD_REQUEST),

    // ==================== RATE LIMIT ERRORS (1090-1099) ====================
    TOO_MANY_REQUESTS(1090, "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    TOO_MANY_LOGIN_ATTEMPTS(1091, "Too many login attempts. Account locked temporarily", HttpStatus.TOO_MANY_REQUESTS),
    RATE_LIMIT_EXCEEDED(1092, "Rate limit exceeded. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    API_QUOTA_EXCEEDED(1093, "API quota exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // ==================== DEVICE ERRORS (1100-1109) ====================
    DEVICE_NOT_FOUND(1100, "Device not found", HttpStatus.NOT_FOUND),
    MAX_DEVICES_EXCEEDED(1101, "Maximum number of devices exceeded", HttpStatus.BAD_REQUEST),
    INVALID_DEVICE(1102, "Invalid device", HttpStatus.BAD_REQUEST),
    DEVICE_TOKEN_INVALID(1103, "Invalid device token", HttpStatus.BAD_REQUEST),
    DEVICE_NOT_TRUSTED(1104, "Device is not trusted", HttpStatus.FORBIDDEN),
    DEVICE_ALREADY_REGISTERED(1105, "Device is already registered", HttpStatus.BAD_REQUEST),

    // ==================== OAUTH/GOOGLE AUTH ERRORS (1110-1119) ====================
    PHONE_REQUIRED_FOR_GOOGLE(1110, "Phone number verification required for Google account", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(1111, "Google authentication failed", HttpStatus.UNAUTHORIZED),
    INVALID_GOOGLE_TOKEN(1112, "Invalid Google access token", HttpStatus.UNAUTHORIZED),
    GOOGLE_ACCOUNT_NOT_LINKED(1113, "Google account not linked", HttpStatus.BAD_REQUEST),
    GOOGLE_ACCOUNT_ALREADY_LINKED(1114, "Google account already linked to another user", HttpStatus.BAD_REQUEST),
    OAUTH_PROVIDER_NOT_SUPPORTED(1115, "OAuth provider not supported", HttpStatus.BAD_REQUEST),
    OAUTH_STATE_MISMATCH(1116, "OAuth state mismatch", HttpStatus.BAD_REQUEST),
    OAUTH_CODE_EXPIRED(1117, "OAuth authorization code expired", HttpStatus.BAD_REQUEST),

    // ==================== PAYMENT ERRORS (1120-1139) ====================
    PAYMENT_REQUIRED(1120, "Payment required", HttpStatus.PAYMENT_REQUIRED),
    INVALID_PAYMENT_METHOD(1121, "Invalid payment method", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(1122, "Payment failed", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(1123, "Insufficient balance", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND(1124, "Transaction not found", HttpStatus.NOT_FOUND),
    REFUND_FAILED(1125, "Refund failed", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_PROCESSED(1126, "Payment already processed", HttpStatus.BAD_REQUEST),

    // ==================== SUBSCRIPTION ERRORS (1140-1149) ====================
    SUBSCRIPTION_NOT_FOUND(1140, "Subscription not found", HttpStatus.NOT_FOUND),
    SUBSCRIPTION_EXPIRED(1141, "Subscription has expired", HttpStatus.FORBIDDEN),
    SUBSCRIPTION_ALREADY_ACTIVE(1142, "Subscription is already active", HttpStatus.BAD_REQUEST),
    SUBSCRIPTION_CANCELLED(1143, "Subscription has been cancelled", HttpStatus.BAD_REQUEST),
    PLAN_NOT_FOUND(1144, "Subscription plan not found", HttpStatus.NOT_FOUND),
    INVALID_SUBSCRIPTION_TIER(1145, "Invalid subscription tier", HttpStatus.BAD_REQUEST),

    // ==================== CONTENT ERRORS (1150-1169) ====================
    CONTENT_NOT_FOUND(1150, "Content not found", HttpStatus.NOT_FOUND),
    CONTENT_UNAVAILABLE(1151, "Content is not available in your region", HttpStatus.FORBIDDEN),
    CONTENT_RESTRICTED(1152, "Content is age-restricted", HttpStatus.FORBIDDEN),
    INVALID_CONTENT_TYPE(1153, "Invalid content type", HttpStatus.BAD_REQUEST),
    CONTENT_ALREADY_EXISTS(1154, "Content already exists", HttpStatus.BAD_REQUEST),

    // ==================== VIDEO/STREAMING ERRORS (1170-1179) ====================
    VIDEO_NOT_FOUND(1170, "Video not found", HttpStatus.NOT_FOUND),
    STREAM_NOT_AVAILABLE(1171, "Stream is not available", HttpStatus.SERVICE_UNAVAILABLE),
    QUALITY_NOT_SUPPORTED(1172, "Video quality not supported", HttpStatus.BAD_REQUEST),
    CONCURRENT_STREAMS_EXCEEDED(1173, "Maximum concurrent streams exceeded", HttpStatus.FORBIDDEN),
    DOWNLOAD_LIMIT_EXCEEDED(1174, "Download limit exceeded", HttpStatus.FORBIDDEN),

    // ==================== COMMENT/REVIEW ERRORS (1180-1189) ====================
    COMMENT_NOT_FOUND(1180, "Comment not found", HttpStatus.NOT_FOUND),
    COMMENT_LOCKED(1181, "Comment section is locked", HttpStatus.FORBIDDEN),
    COMMENT_TOO_LONG(1182, "Comment exceeds maximum length", HttpStatus.BAD_REQUEST),
    SPAM_DETECTED(1183, "Spam detected in content", HttpStatus.BAD_REQUEST),

    // ==================== WATCHLIST/FAVORITE ERRORS (1190-1199) ====================
    WATCHLIST_FULL(1190, "Watchlist is full", HttpStatus.BAD_REQUEST),
    ALREADY_IN_WATCHLIST(1191, "Item already in watchlist", HttpStatus.BAD_REQUEST),
    NOT_IN_WATCHLIST(1192, "Item not in watchlist", HttpStatus.NOT_FOUND),

    // ==================== NOTIFICATION ERRORS (1200-1209) ====================
    NOTIFICATION_NOT_FOUND(1200, "Notification not found", HttpStatus.NOT_FOUND),
    NOTIFICATION_SEND_FAILED(1201, "Failed to send notification", HttpStatus.INTERNAL_SERVER_ERROR),
    PUSH_TOKEN_INVALID(1202, "Invalid push notification token", HttpStatus.BAD_REQUEST),

    // ==================== ADMIN/MODERATION ERRORS (1210-1219) ====================
    ADMIN_ACTION_REQUIRED(1210, "Admin action required", HttpStatus.FORBIDDEN),
    CONTENT_UNDER_REVIEW(1211, "Content is under review", HttpStatus.FORBIDDEN),
    REPORT_NOT_FOUND(1212, "Report not found", HttpStatus.NOT_FOUND),
    ALREADY_REPORTED(1213, "Content already reported", HttpStatus.BAD_REQUEST),

    // ==================== EXTERNAL SERVICE ERRORS (1220-1229) ====================
    EXTERNAL_SERVICE_ERROR(1220, "External service error", HttpStatus.BAD_GATEWAY),
    CDN_ERROR(1221, "CDN service error", HttpStatus.BAD_GATEWAY),
    PAYMENT_GATEWAY_ERROR(1222, "Payment gateway error", HttpStatus.BAD_GATEWAY),
    EMAIL_SERVICE_ERROR(1223, "Email service error", HttpStatus.INTERNAL_SERVER_ERROR),
    SMS_SERVICE_ERROR(1224, "SMS service error", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== NETWORK/CONNECTION ERRORS (1230-1239) ====================
    NETWORK_ERROR(1230, "Network error occurred", HttpStatus.SERVICE_UNAVAILABLE),
    TIMEOUT_ERROR(1231, "Request timeout", HttpStatus.REQUEST_TIMEOUT),
    CONNECTION_REFUSED(1232, "Connection refused", HttpStatus.SERVICE_UNAVAILABLE),

    // ==================== DATABASE ERRORS (1240-1249) ====================
    DATABASE_ERROR(1240, "Database error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    DUPLICATE_ENTRY(1241, "Duplicate entry", HttpStatus.CONFLICT),
    CONSTRAINT_VIOLATION(1242, "Database constraint violation", HttpStatus.BAD_REQUEST),

    // ==================== BUSINESS LOGIC ERRORS (1250-1299) ====================
    OPERATION_NOT_ALLOWED(1250, "Operation not allowed", HttpStatus.FORBIDDEN),
    INVALID_STATE_TRANSITION(1251, "Invalid state transition", HttpStatus.BAD_REQUEST),
    RESOURCE_LOCKED(1252, "Resource is locked", HttpStatus.LOCKED),
    DEPENDENCY_NOT_MET(1253, "Required dependency not met", HttpStatus.BAD_REQUEST),
    CONFLICT(1254, "Resource conflict", HttpStatus.CONFLICT);

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}