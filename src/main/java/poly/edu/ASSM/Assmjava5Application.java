package poly.edu.ASSM;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;

/**
 * Sports4Life — entry point ứng dụng Spring Boot.
 */
@SpringBootApplication
public class Assmjava5Application {

	public static void main(String[] args) {
		Environment env = new SpringApplicationBuilder(Assmjava5Application.class)
				.logStartupInfo(true)
				.run(args)
				.getEnvironment();

		String port = env.getProperty("server.port", "8080");
		String context = env.getProperty("server.servlet.context-path", "");
		String url = "http://localhost:" + port + context;
		System.out.println();
		System.out.println("  \u001B[92m┌──────────────────────────────────────────────┐\u001B[0m");
		System.out.println("  \u001B[92m│\u001B[0m  \u001B[92m●\u001B[0m Sports4Life sẵn sàng                   \u001B[92m│\u001B[0m");
		System.out.println("  \u001B[92m│\u001B[0m  \u001B[93m→\u001B[0m Local: " + padRight(url, 34) + "\u001B[92m│\u001B[0m");
		System.out.println("  \u001B[92m└──────────────────────────────────────────────┘\u001B[0m");
		System.out.println();
	}

	private static String padRight(String value, int width) {
		if (value.length() >= width) {
			return value.substring(0, width);
		}
		return value + " ".repeat(width - value.length());
	}

}