package mediaservice.dtos.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceUserResponse {
    private String id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String coverUrl;
}
