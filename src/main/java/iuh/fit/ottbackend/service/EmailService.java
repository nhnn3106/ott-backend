package iuh.fit.ottbackend.service;

import iuh.fit.ottbackend.entity.enums.OtpType;
import iuh.fit.ottbackend.exception.AppException;
import iuh.fit.ottbackend.exception.ErrorCode;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.name}")
    private String appName;

    @Value("${app.website}")
    private String websiteUrl;

    @Value("${app.support-email}")
    private String supportEmail;

    @Value("${otp.expiry-minutes}")
    private int otpExpiryMinutes;

    public void sendOtpEmail(String to, String fullName, String otpCode, OtpType otpType,
                             String ipAddress, String location) {

        String subject = getSubjectByOtpType(otpType);
        String templateName = getTemplateByOtpType(otpType);

        Context context = new Context();
        context.setVariable("appName", appName);
        context.setVariable("fullName", fullName != null ? fullName : "User");
        context.setVariable("otpCode", otpCode);
        context.setVariable("expiryMinutes", otpExpiryMinutes);
        context.setVariable("websiteUrl", websiteUrl);
        context.setVariable("supportEmail", supportEmail);
        context.setVariable("timestamp", LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")));

        if (ipAddress != null) {
            context.setVariable("ipAddress", ipAddress);
        }
        if (location != null) {
            context.setVariable("location", location);
        }


        String htmlContent = templateEngine.process(templateName, context);

        sendHtmlEmail(to, subject, htmlContent);

        log.info("OTP email sent successfully to {} for type {}", to, otpType);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", to, e.getMessage());
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    private String getSubjectByOtpType(OtpType otpType) {
        return switch (otpType) {
            case LOGIN_OTP_EMAIL -> "Your Login Verification Code - " + appName;
            case EMAIL_VERIFICATION -> "Verify Your Email Address - " + appName;
            case RESET_PASSWORD -> "Reset Your Password - " + appName;
            case CHANGE_EMAIL -> "Confirm Your New Email - " + appName;
            default -> "Your Verification Code - " + appName;
        };
    }

    private String getTemplateByOtpType(OtpType otpType) {
        return switch (otpType) {
            case LOGIN_OTP_EMAIL, EMAIL_VERIFICATION, RESET_PASSWORD, CHANGE_EMAIL
                    -> "email/otp-login";
            default -> "email/otp-login";
        };
    }
}
