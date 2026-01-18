package iuh.fit.ottbackend.controller;

import iuh.fit.ottbackend.dto.request.*;
import iuh.fit.ottbackend.dto.response.ApiResponse;
import iuh.fit.ottbackend.dto.response.OtpResponse;
import iuh.fit.ottbackend.service.AccountService;
import iuh.fit.ottbackend.utils.ControllerUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;
    private final ControllerUtils controllerUtils;

    @PostMapping("/password/set")
    public ApiResponse<Void> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        String userId = controllerUtils.getCurrentUserId();
        accountService.setPassword(userId, request);

        log.info("Password set for user: {}", userId);

        return ApiResponse.<Void>builder()
                .message("Password set successfully")
                .build();
    }

    @PostMapping("/password/change")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        String userId = controllerUtils.getCurrentUserId();
        accountService.changePassword(userId, request);

        log.info("Password changed for user: {}", userId);

        return ApiResponse.<Void>builder()
                .message("Password changed successfully. All sessions have been revoked.")
                .build();
    }

    @PostMapping("/password/forgot/request")
    public ApiResponse<OtpResponse> requestPasswordReset(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        OtpResponse response = accountService.requestPasswordReset(request);

        log.info("Password reset OTP requested for phone: {}, email: {}",
                request.getPhone(), request.getEmail());

        return ApiResponse.<OtpResponse>builder()
                .message("OTP has been sent to your email address")
                .result(response)
                .build();
    }

    @PostMapping("/password/forgot/verify")
    public ApiResponse<Void> verifyPasswordReset(
            @Valid @RequestBody VerifyPasswordResetRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);
        accountService.verifyPasswordReset(request);

        log.info("Password reset successfully for phone: {}, email: {}",
                request.getPhone(), request.getEmail());

        return ApiResponse.<Void>builder()
                .message("Password reset successfully. Please login with your new password.")
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        String userId = controllerUtils.getCurrentUserId();
        accountService.deleteAccount(userId, request);

        log.info("Account deleted for user: {}", userId);

        return ApiResponse.<Void>builder()
                .message("Account deleted successfully")
                .build();
    }
}