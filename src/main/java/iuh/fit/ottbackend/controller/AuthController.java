package iuh.fit.ottbackend.controller;

import com.nimbusds.jose.JOSEException;
import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.*;
import iuh.fit.ottbackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthController {

    AuthService authService;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        enrichRequestWithClientInfo(request, httpRequest);

        AuthenticationResponse response = authService.authenticate(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Login successful")
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> googleAuthenticate(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpRequest) {

        enrichRequestWithClientInfo(request, httpRequest);

        AuthenticationResponse response = authService.googleAuthenticate(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Google login successful")
                .build();
    }

    @PostMapping("/google/link")
    public ApiResponse<AuthenticationResponse> linkGoogleWithPhone(
            @Valid @RequestBody LinkGoogleRequest request,
            HttpServletRequest httpRequest) {

        enrichRequestWithClientInfo(request, httpRequest);

        AuthenticationResponse response = authService.linkGoogleWithPhone(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Google account linked successfully")
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(
            @Valid @RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {

        IntrospectResponse response = authService.introspect(request);

        return ApiResponse.<IntrospectResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(
            @Valid @RequestBody RefreshRequest request)
            throws ParseException, JOSEException {

        AuthenticationResponse response = authService.refreshToken(request);

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Token refreshed successfully")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody LogoutRequest request)
            throws ParseException, JOSEException {

        authService.logout(request);

        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }

    @PostMapping("/qr/generate")
    public ApiResponse<QrCodeResponse> generateQrCode(
            @Valid @RequestBody QrGenerateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        request.setIpAddress(ipAddress);

        if (request.getDeviceInfo() == null) {
            request.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        }

        QrCodeResponse response = authService.generateLoginQrCode(request);

        return ApiResponse.<QrCodeResponse>builder()
                .result(response)
                .message("QR code generated successfully")
                .build();
    }

    @PostMapping("/qr/scan")
    public ApiResponse<QrStatusResponse> scanQrCode(
            @Valid @RequestBody QrScanRequest request,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId();
        String ipAddress = getClientIp(httpRequest);
        request.setIpAddress(ipAddress);

        if (request.getDeviceInfo() == null) {
            request.setDeviceInfo(httpRequest.getHeader("User-Agent"));
        }

        QrStatusResponse response = authService.scanQrCode(request, userId);

        return ApiResponse.<QrStatusResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/qr/confirm")
    public ApiResponse<QrStatusResponse> confirmQrLogin(
            @Valid @RequestBody QrConfirmRequest request) {

        String userId = getCurrentUserId();
        QrStatusResponse response = authService.confirmQrLogin(request, userId);

        return ApiResponse.<QrStatusResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping("/qr/status/{qrId}")
    public ApiResponse<QrStatusResponse> checkQrStatus(@PathVariable String qrId) {

        QrStatusResponse response = authService.checkQrStatus(qrId);

        return ApiResponse.<QrStatusResponse>builder()
                .result(response)
                .build();
    }

    @DeleteMapping("/qr/{qrId}")
    public ApiResponse<Void> cancelQrCode(@PathVariable String qrId) {

        authService.cancelQrCode(qrId);

        return ApiResponse.<Void>builder()
                .message("QR code cancelled successfully")
                .build();
    }

    private void enrichRequestWithClientInfo(Object request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        if (request instanceof AuthenticationRequest) {
            AuthenticationRequest authRequest = (AuthenticationRequest) request;
            authRequest.setIpAddress(ipAddress);
            if (authRequest.getDeviceInfo() == null) {
                authRequest.setDeviceInfo(userAgent);
            }
        } else if (request instanceof GoogleAuthRequest) {
            GoogleAuthRequest googleRequest = (GoogleAuthRequest) request;
            googleRequest.setIpAddress(ipAddress);
            if (googleRequest.getDeviceInfo() == null) {
                googleRequest.setDeviceInfo(userAgent);
            }
        } else if (request instanceof LinkGoogleRequest) {
            LinkGoogleRequest linkRequest = (LinkGoogleRequest) request;
            linkRequest.setIpAddress(ipAddress);
            if (linkRequest.getDeviceInfo() == null) {
                linkRequest.setDeviceInfo(userAgent);
            }
        }
    }


    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        return authentication.getName();
    }
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}