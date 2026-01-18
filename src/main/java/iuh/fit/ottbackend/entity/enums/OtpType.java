package iuh.fit.ottbackend.entity.enums;

import lombok.Getter;

@Getter
public enum OtpType {
    LOGIN_OTP_EMAIL("login_otp_email"),
    EMAIL_VERIFICATION("email_verification"),
    RESET_PASSWORD("reset_password"),
    CHANGE_EMAIL("change_email"),
    LINK_GOOGLE_ACCOUNT("link_google_account"),
    TWO_FACTOR_AUTH("two_factor_auth"),
    LINK_PHONE("link_phone"),
    LINK_EMAIL("link_email"),
    REGISTER("register")
    ;

    private final String value;

    OtpType(String value) {
        this.value = value;
    }
}