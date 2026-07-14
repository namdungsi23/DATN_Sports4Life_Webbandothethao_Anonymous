package poly.edu.ASSM.security;

/**
 * Chính sách mật khẩu dùng chung cho đăng ký / đổi mật khẩu / quên mật khẩu.
 * Yêu cầu: ≥8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt.
 */
public final class PasswordPolicy {

	private PasswordPolicy() {
	}

	public static final int MIN_LENGTH = 8;
	public static final int MAX_LENGTH = 72;

	/**
	 * @return null nếu hợp lệ, ngược lại thông báo lỗi tiếng Việt
	 */
	public static String validate(String password) {
		if (password == null || password.isBlank()) {
			return "Mật khẩu là bắt buộc.";
		}
		if (password.length() < MIN_LENGTH) {
			return "Mật khẩu tối thiểu " + MIN_LENGTH + " ký tự.";
		}
		if (password.length() > MAX_LENGTH) {
			return "Mật khẩu tối đa " + MAX_LENGTH + " ký tự.";
		}

		boolean hasUpper = false;
		boolean hasLower = false;
		boolean hasDigit = false;
		boolean hasSpecial = false;

		for (int i = 0; i < password.length(); i++) {
			char c = password.charAt(i);
			if (Character.isUpperCase(c)) {
				hasUpper = true;
			} else if (Character.isLowerCase(c)) {
				hasLower = true;
			} else if (Character.isDigit(c)) {
				hasDigit = true;
			} else if (!Character.isWhitespace(c)) {
				// Ký tự đặc biệt: không phải chữ/số/khoảng trắng
				hasSpecial = true;
			}
		}

		if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
			return "Mật khẩu phải có chữ hoa, chữ thường, số và ký tự đặc biệt.";
		}
		return null;
	}

	public static boolean isValid(String password) {
		return validate(password) == null;
	}
}
