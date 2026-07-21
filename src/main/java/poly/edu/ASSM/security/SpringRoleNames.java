package poly.edu.ASSM.security;

/** Spring {@code hasRole("ADMIN")} requires authority {@code ROLE_ADMIN}. */
public final class SpringRoleNames {

	private SpringRoleNames() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String s = raw.trim().toUpperCase();
		if (s.isEmpty()) {
			return "";
		}
		return s.startsWith("ROLE_") ? s : "ROLE_" + s;
	}
}
