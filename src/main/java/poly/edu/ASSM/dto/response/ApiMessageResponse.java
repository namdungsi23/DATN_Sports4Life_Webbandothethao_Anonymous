package poly.edu.ASSM.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiMessageResponse {
    private Boolean ok;
    private String message;
    private String username;
    private Boolean needsVerification;
    private String verifyChannel;
    private String destination;
    private Integer otpTtlSeconds;
    private String devOtp;
    private String devNote;
    private String smsError;
    private Boolean alreadySubscribed;
    private String resetToken;
}
