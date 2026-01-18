package iuh.fit.ottbackend.dto.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleLinkRequiredResponse {
    private String email;
    private String fullName;
    private String avatarUrl;
    private String googleId;
    private boolean phoneRequired;
    private String message;
}