package poly.edu.ASSM.mapper;

import java.util.Map;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.dto.response.ApiMessageResponse;

@Component
public class ApiMessageMapper {

    public ApiMessageResponse fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return ApiMessageResponse.builder()
                .ok(boolValue(body.get("ok")))
                .message(stringValue(body.get("message")))
                .username(stringValue(body.get("username")))
                .needsVerification(boolValue(body.get("needsVerification")))
                .verifyChannel(stringValue(body.get("verifyChannel")))
                .destination(stringValue(body.get("destination")))
                .otpTtlSeconds(intValue(body.get("otpTtlSeconds")))
                .devOtp(stringValue(body.get("devOtp")))
                .devNote(stringValue(body.get("devNote")))
                .smsError(stringValue(body.get("smsError")))
                .alreadySubscribed(boolValue(body.get("alreadySubscribed")))
                .resetToken(stringValue(body.get("resetToken") != null ? body.get("resetToken") : body.get("token")))
                .build();
    }

    public ApiMessageResponse ok(String message) {
        return ApiMessageResponse.builder().ok(true).message(message).build();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Boolean boolValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
