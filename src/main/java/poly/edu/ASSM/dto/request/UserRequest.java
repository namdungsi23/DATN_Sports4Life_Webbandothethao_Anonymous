package poly.edu.ASSM.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequest {

    private Integer id;
    private Long accountId;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    private LocalDate dateOfBirth;
    private Integer totalPoint;
    private Integer rankId;
}
