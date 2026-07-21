package poly.edu.ASSM.security;

/**
 * Lớp 3 + 5: chính sách mật khẩu dùng chung cho đăng ký / đổi mật khẩu.
 */
public final class PasswordPolicy {

	private PasswordPolicy() {
	}

	public static final int MIN_LENGTH = 8;

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
		if (password.length() > 72) {
			return "Mật khẩu tối đa 72 ký tự.";
		}
		boolean hasLetter = password.chars().anyMatch(Character::isLetter);
		boolean hasDigit = password.chars().anyMatch(Character::isDigit);
		if (!hasLetter || !hasDigit) {
			return "Mật khẩu phải gồm ít nhất 1 chữ cái và 1 chữ số.";
		}
		return null;
	}

	public static boolean isValid(String password) {
		return validate(password) == null;
	}
}
