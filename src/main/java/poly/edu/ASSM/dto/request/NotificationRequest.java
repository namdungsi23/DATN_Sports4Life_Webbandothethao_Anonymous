package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationRequest {

    private Integer id;

    @NotNull(message = "UserId không được để trống")
    private Integer userId;

    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    private String message;
    private Boolean isRead;
}
