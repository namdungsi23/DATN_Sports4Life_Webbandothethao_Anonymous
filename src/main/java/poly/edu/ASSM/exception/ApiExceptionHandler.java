package poly.edu.ASSM.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Chuẩn hóa JSON lỗi validate / input cho FE:
 * { "ok": false, "message": "...", "errors": { "field": "msg" } }
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!fieldErrors.containsKey(error.getField())) {
                fieldErrors.put(error.getField(), error.getDefaultMessage());
            }
        }
        String message = fieldErrors.isEmpty()
                ? "Dữ liệu không hợp lệ."
                : fieldErrors.values().iterator().next();
        return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (!fieldErrors.containsKey(error.getField())) {
                fieldErrors.put(error.getField(), error.getDefaultMessage());
            }
        }
        String message = fieldErrors.isEmpty()
                ? "Dữ liệu không hợp lệ."
                : fieldErrors.values().iterator().next();
        return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidInput(InvalidInputException ex) {
        return ResponseEntity.badRequest().body(errorBody(
                ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ.",
                Map.of()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.BAD_REQUEST;
        }
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        return ResponseEntity.status(status).body(errorBody(message, Map.of()));
    }

    /** Lớp 5 — không lộ stack trace ra client */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnhandled(Exception ex) {
        if (ex instanceof org.springframework.security.access.AccessDeniedException
                || ex instanceof org.springframework.security.core.AuthenticationException) {
            throw (RuntimeException) ex;
        }
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
                .error("Unhandled API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", Map.of()));
    }

    private static Map<String, Object> errorBody(String message, Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", false);
        body.put("message", message);
        body.put("errors", errors != null ? errors : Map.of());
        return body;
    }
}
