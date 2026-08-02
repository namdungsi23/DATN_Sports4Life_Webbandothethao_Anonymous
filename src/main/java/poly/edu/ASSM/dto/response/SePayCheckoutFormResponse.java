package poly.edu.ASSM.dto.response;

import java.util.Map;

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
public class SePayCheckoutFormResponse {
    private String action;
    private String method;
    private Map<String, String> fields;
    private String invoiceNumber;
    private Long amount;
    private String env;
    private String ipnUrl;
}
