package iuh.fit.ottbackend.entity.enums;

import lombok.Getter;

@Getter
public enum OtpType {
    LOGIN("login"),
    REGISTER("register"),
    RESET_PASSWORD("reset_password"),
    VERIFY_PHONE("verify_phone"),
    VERIFY_EMAIL("verify_email"),
    CHANGE_PHONE("change_phone"),
    LINK_ACCOUNT("link_account")
    ;

    private final String value;

    OtpType(String value) {
        this.value = value;
    }
}
