package iuh.fit.ottbackend.controller;

import com.nimbusds.jose.JOSEException;
import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.ApiResponse;
import iuh.fit.ottbackend.dto.response.AuthenticationResponse;
import iuh.fit.ottbackend.dto.response.IntrospectResponse;
import iuh.fit.ottbackend.dto.response.OtpResponse;
import iuh.fit.ottbackend.service.AuthService;
import iuh.fit.ottbackend.service.JwtService;
import iuh.fit.ottbackend.utils.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final ControllerUtils controllerUtils;

    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        AuthenticationResponse response = authService.authenticate(request);

        log.info("User logged in successfully with phone: {}", request.getPhone());

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Login successful")
                .build();
    }

    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> googleAuthenticate(
            @Valid @RequestBody GoogleAuthRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        AuthenticationResponse response = authService.googleAuthenticate(request);

        log.info("User logged in successfully with Google");

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Google login successful")
                .build();
    }

    @PostMapping("/google/link")
    public ApiResponse<AuthenticationResponse> linkGoogleWithPhone(
            @Valid @RequestBody LinkGoogleRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        AuthenticationResponse response = authService.linkGoogleWithPhone(request);

        log.info("Google account linked with phone: {}", request.getPhone());

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Google account linked and logged in successfully")
                .build();
    }

    @PostMapping("/otp/email/request")
    public ApiResponse<OtpResponse> requestEmailOtp(
            @Valid @RequestBody EmailOtpRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        OtpResponse response = authService.requestEmailOtp(request);

        log.info("Email OTP requested for: {}", request.getEmail());

        return ApiResponse.<OtpResponse>builder()
                .message("OTP has been sent to your email")
                .result(response)
                .build();
    }

    @PostMapping("/otp/email/verify")
    public ApiResponse<AuthenticationResponse> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        AuthenticationResponse response = authService.verifyEmailOtp(request);

        log.info("User logged in successfully with email OTP: {}", request.getEmail());

        return ApiResponse.<AuthenticationResponse>builder()
                .message("Login successful")
                .result(response)
                .build();
    }

    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(
            @Valid @RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {

        IntrospectResponse response = jwtService.introspect(request);

        return ApiResponse.<IntrospectResponse>builder()
                .result(response)
                .build();
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(
            @Valid @RequestBody RefreshRequest request)
            throws ParseException, JOSEException {

        AuthenticationResponse response = authService.refreshToken(request);

        log.info("Token refreshed successfully");

        return ApiResponse.<AuthenticationResponse>builder()
                .result(response)
                .message("Token refreshed successfully")
                .build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request)
            throws ParseException, JOSEException {

        authService.logout(request);

        log.info("User logged out successfully");

        return ApiResponse.<Void>builder()
                .message("Logout successful")
                .build();
    }
}