package iuh.fit.ottbackend.controller;

import iuh.fit.ottbackend.dto.request.RequestEmailOtpRequest;
import iuh.fit.ottbackend.dto.request.RequestPhoneOtpRequest;
import iuh.fit.ottbackend.dto.response.ApiResponse;
import iuh.fit.ottbackend.dto.response.OtpResponse;
import iuh.fit.ottbackend.entity.OtpCode;
import iuh.fit.ottbackend.entity.enums.OtpType;
import iuh.fit.ottbackend.service.EmailService;
import iuh.fit.ottbackend.service.OtpService;
import iuh.fit.ottbackend.utils.ControllerUtils;
import iuh.fit.ottbackend.utils.ValidationUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
@Slf4j
public class OtpController {

    private final OtpService otpService;
    private final EmailService emailService;
    private final ControllerUtils controllerUtils;
    private final ValidationUtils validationUtils;

    @PostMapping("/register/phone")
    public ApiResponse<OtpResponse> requestRegisterPhoneOtp(
            @Valid @RequestBody RequestPhoneOtpRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);

        OtpCode otpCode = otpService.generateOtp(
                request.getPhone(),
                null,
                OtpType.REGISTER,
                request.getIpAddress()
        );

        log.info("Registration OTP generated for phone: {} - Code: {}",
                request.getPhone(), otpCode.getCode());

        return ApiResponse.<OtpResponse>builder()
                .message("OTP sent successfully")
                .result(OtpResponse.builder()
                        .phone(request.getPhone())
                        .expiresAt(otpCode.getExpiresAt())
                        .message("Please check your email for the OTP code")
                        .build())
                .build();
    }
    @PostMapping("/link/phone")
    public ApiResponse<OtpResponse> requestLinkPhoneOtp(
            @Valid @RequestBody RequestPhoneOtpRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);

        OtpCode otpCode = otpService.generateOtp(
                request.getPhone(),
                null,
                OtpType.LINK_PHONE,
                request.getIpAddress()
        );

        log.info("Link phone OTP generated for phone: {} - Code: {}",
                request.getPhone(), otpCode.getCode());

        return ApiResponse.<OtpResponse>builder()
                .message("OTP sent successfully")
                .result(OtpResponse.builder()
                        .phone(request.getPhone())
                        .expiresAt(otpCode.getExpiresAt())
                        .message("Please check your email for the OTP code")
                        .build())
                .build();
    }

    @PostMapping("/link/email")
    public ApiResponse<OtpResponse> requestLinkEmailOtp(
            @Valid @RequestBody RequestEmailOtpRequest request,
            HttpServletRequest httpRequest) {

        controllerUtils.enrichWithClientInfo(request, httpRequest);

        OtpCode otpCode = otpService.generateOtp(
                null,
                request.getEmail(),
                OtpType.LINK_EMAIL,
                request.getIpAddress()
        );

        emailService.sendOtpEmail(
                request.getEmail(),
                "User",
                otpCode.getCode(),
                OtpType.LINK_EMAIL,
                request.getIpAddress(),
                null
        );

        log.info("Link email OTP sent to: {}", validationUtils.maskEmail(request.getEmail()));

        return ApiResponse.<OtpResponse>builder()
                .message("OTP sent successfully")
                .result(OtpResponse.builder()
                        .email(validationUtils.maskEmail(request.getEmail()))
                        .expiresAt(otpCode.getExpiresAt())
                        .message("Please check your email for the OTP code")
                        .build())
                .build();
    }
}