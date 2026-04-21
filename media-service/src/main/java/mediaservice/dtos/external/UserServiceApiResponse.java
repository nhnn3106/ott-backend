package mediaservice.dtos.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserServiceApiResponse {
    private int code;
    private String message;
    private UserServiceUserResponse result;
}
