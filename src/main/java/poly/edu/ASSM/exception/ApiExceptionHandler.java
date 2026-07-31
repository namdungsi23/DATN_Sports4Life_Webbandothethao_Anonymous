package poly.edu.ASSM.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Chuẩn hóa JSON lỗi cho FE:
 * {@code { "ok": false, "message": "...", "errors": { "field": "msg" } }}
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = collectFieldErrors(ex.getBindingResult().getFieldErrors());
		String message = firstMessage(fieldErrors, "Dữ liệu không hợp lệ.");
		return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
		Map<String, String> fieldErrors = collectFieldErrors(ex.getBindingResult().getFieldErrors());
		String message = firstMessage(fieldErrors, "Dữ liệu không hợp lệ.");
		return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
			String path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "value";
			String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
			if (!fieldErrors.containsKey(field)) {
				fieldErrors.put(field, v.getMessage());
			}
		}
		String message = firstMessage(fieldErrors, "Dữ liệu không hợp lệ.");
		return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
	}

	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidInput(InvalidInputException ex) {
		Map<String, String> fieldErrors = ex.getErrors() != null ? ex.getErrors() : Map.of();
		String message = ex.getMessage() != null && !ex.getMessage().isBlank()
				? ex.getMessage()
				: firstMessage(fieldErrors, "Dữ liệu không hợp lệ.");
		return ResponseEntity.badRequest().body(errorBody(message, fieldErrors));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
		String field = ex.getParameterName();
		String message = "Thiếu tham số: " + field;
		return ResponseEntity.badRequest().body(errorBody(message, Map.of(field, message)));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String field = ex.getName() != null ? ex.getName() : "value";
		String message = "Giá trị không hợp lệ cho trường " + field + ".";
		return ResponseEntity.badRequest().body(errorBody(message, Map.of(field, message)));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest().body(errorBody("Dữ liệu gửi lên không hợp lệ.", Map.of()));
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

	private static Map<String, String> collectFieldErrors(Iterable<FieldError> fieldErrors) {
		Map<String, String> errors = new LinkedHashMap<>();
		for (FieldError error : fieldErrors) {
			if (!errors.containsKey(error.getField())) {
				errors.put(error.getField(), error.getDefaultMessage());
			}
		}
		return errors;
	}

	private static String firstMessage(Map<String, String> fieldErrors, String fallback) {
		if (fieldErrors == null || fieldErrors.isEmpty()) {
			return fallback;
		}
		return fieldErrors.values().iterator().next();
	}

	private static Map<String, Object> errorBody(String message, Map<String, String> errors) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", false);
		body.put("message", message != null ? message : "Có lỗi xảy ra.");
		body.put("errors", errors != null ? errors : Map.of());
		return body;
	}
}
