package poly.edu.ASSM.Services.Config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import poly.edu.ASSM.Services.util.JwtAuthFilter;
import poly.edu.ASSM.security.CustomAccessDeniedHandler;
import poly.edu.ASSM.security.CustomAuthenticationEntryPoint;
import poly.edu.ASSM.security.CustomAuthenticationFailureHandler;
import poly.edu.ASSM.security.CustomSuccessHandler;
import poly.edu.ASSM.security.LoginRateLimitFilter;
import poly.edu.ASSM.security.OAuth2LoginFailureHandler;
import poly.edu.ASSM.security.OAuth2LoginSuccessHandler;
import poly.edu.ASSM.security.SecurityHeadersFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	@Autowired
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@Autowired
	OAuth2LoginFailureHandler oAuth2LoginFailureHandler;

	@Autowired
	CustomSuccessHandler customSuccessHandler;

	@Autowired
	CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

	@Autowired
	CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

	@Autowired
	CustomAccessDeniedHandler customAccessDeniedHandler;

	@Autowired
	JwtAuthFilter jwtAuthFilter;

	@Autowired
	LoginRateLimitFilter loginRateLimitFilter;

	@Autowired
	SecurityHeadersFilter securityHeadersFilter;

	@Value("${app.security.cors.allowed-origins:http://localhost:5173,http://localhost:5174,http://127.0.0.1:5173,http://127.0.0.1:5174}")
	private String corsAllowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();

		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
						.requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
						.requestMatchers(
								"/",
								"/api/public/**",
								"/api/auth/refresh",
								"/home",
								"/product",
								"/login",
								"/error",
								"/oauth2/**",
								"/login/oauth2/**",
								"/css/**",
								"/js/**",
								"/images/**")
						.permitAll()
						.anyRequest().authenticated())
				.cors(cors -> cors.configurationSource(request -> {
					CorsConfiguration config = new CorsConfiguration();
					config.setAllowedOrigins(origins);
					config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
					config.setAllowedHeaders(List.of("*"));
					config.setAllowCredentials(true);
					config.setMaxAge(3600L);
					return config;
				}))
				// SPA dùng Bearer JWT → CSRF tắt; không dựa cookie session cho API
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.formLogin(form -> form
						.loginPage("/")
						.loginProcessingUrl("/login/validate")
						.successHandler(customSuccessHandler)
						.failureHandler(customAuthenticationFailureHandler)
						.usernameParameter("username")
						.passwordParameter("pwd"))
				.oauth2Login(oauth -> oauth
						.successHandler(oAuth2LoginSuccessHandler)
						.failureHandler(oAuth2LoginFailureHandler))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(customAuthenticationEntryPoint)
						.accessDeniedHandler(customAccessDeniedHandler))
				.rememberMe(reme -> reme.disable());

		return http.build();
	}
}
