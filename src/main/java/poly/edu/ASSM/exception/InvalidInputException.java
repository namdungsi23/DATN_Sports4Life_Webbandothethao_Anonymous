package poly.edu.ASSM.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lỗi nghiệp vụ / input từ service — {@link ApiExceptionHandler} map sang JSON cho FE:
 * {@code { "ok": false, "message": "...", "errors": { "field": "msg" } }}
 */
public class InvalidInputException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final Map<String, String> errors;

	public InvalidInputException(String message) {
		super(message);
		this.errors = Collections.emptyMap();
	}

	public InvalidInputException(String message, Throwable cause) {
		super(message, cause);
		this.errors = Collections.emptyMap();
	}

	public InvalidInputException(Throwable cause) {
		super(cause);
		this.errors = Collections.emptyMap();
	}

	/** Lỗi gắn 1 field — FE hiển thị dưới ô tương ứng. */
	public InvalidInputException(String field, String message) {
		super(message);
		Map<String, String> map = new LinkedHashMap<>();
		if (field != null && !field.isBlank() && message != null) {
			map.put(field, message);
		}
		this.errors = Collections.unmodifiableMap(map);
	}

	public InvalidInputException(String message, Map<String, String> errors) {
		super(message);
		this.errors = errors == null || errors.isEmpty()
				? Collections.emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<>(errors));
	}

	public static InvalidInputException of(String field, String message) {
		return new InvalidInputException(field, message);
	}

	public Map<String, String> getErrors() {
		return errors;
	}
}
