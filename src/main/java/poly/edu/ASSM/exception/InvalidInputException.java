package poly.edu.ASSM.exception;

/**
 * Thrown when user input or request data fails validation (e.g. invalid format,
 * missing required fields, or out-of-range values).
 */
public class InvalidInputException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public InvalidInputException(String message) {
		super(message);
	}

	public InvalidInputException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidInputException(Throwable cause) {
		super(cause);
	}
}
