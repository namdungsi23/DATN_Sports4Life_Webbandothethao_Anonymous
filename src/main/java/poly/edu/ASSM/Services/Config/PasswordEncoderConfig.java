package poly.edu.ASSM.Services.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Delegates to Spring’s standard {@link PasswordEncoderFactories} so any stored
 * {@code {id}…} form (e.g. {@code {bcrypt}}, {@code {argon2}}, {@code {pbkdf2}}) matches at login.
 * Legacy rows with no prefix (plain text from old seeds) are treated as {@code {noop}}.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();

		return new PasswordEncoder() {
			@Override
			public String encode(CharSequence rawPassword) {
				return delegating.encode(rawPassword);
			}

			@Override
			public boolean matches(CharSequence rawPassword, String encodedPassword) {
				if (encodedPassword == null || encodedPassword.isEmpty()) {
					return false;
				}
				if (encodedPassword.startsWith("{")) {
					return delegating.matches(rawPassword, encodedPassword);
				}
				return delegating.matches(rawPassword, "{noop}" + encodedPassword);
			}

			@Override
			public boolean upgradeEncoding(String encodedPassword) {
				return delegating.upgradeEncoding(encodedPassword);
			}
		};
	}
}
