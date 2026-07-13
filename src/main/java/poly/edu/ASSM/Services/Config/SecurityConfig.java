package poly.edu.ASSM.Services.Config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import poly.edu.ASSM.Services.util.JwtAuthFilter;
import poly.edu.ASSM.component.CustomAccessDeniedHandler;
import poly.edu.ASSM.component.CustomAuthenticationEntryPoint;
import poly.edu.ASSM.component.CustomFormLoginFailureHandler;
import poly.edu.ASSM.component.CustomSuccessHandler;
import poly.edu.ASSM.component.OAuth2LoginFailureHandler;
import poly.edu.ASSM.component.OAuth2LoginSuccessHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	@Autowired
	OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

	@Autowired
	OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
	
	@Autowired
	CustomFormLoginFailureHandler customFormLoginFailureHandler;
	
	@Autowired
	CustomSuccessHandler customSuccessHandler;
	
	@Autowired
	CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
	
	@Autowired
	CustomAccessDeniedHandler customAccessDeniedHandler;
	
	@Autowired
	JwtAuthFilter jwtAuthFilter;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Bean
	public AuthenticationManager authenticationManager() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(passwordEncoder);
		provider.setUserDetailsService(userDetailsService);
		return new ProviderManager(provider);
	}
	
	/*
	@Bean
    public PasswordEncoder getPasswordEncoder() {
    	return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
    */

	
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
	    http
	        .authorizeHttpRequests(auth -> auth
	            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
		        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "STAFF")
	            .requestMatchers("/api/admin/**").authenticated()
	            .requestMatchers(
	            	"/",
	            	"/api/public/**",
	            	"/home",
	            	"/product",
	                "/login", 
	                "/error",
	                "/oauth2/**",
	                "/login/oauth2/**",
	                "/css/**",
	                "/js/**",
	                "/images/**"
	            ).permitAll()
	            .anyRequest().authenticated()
	        )
	        .cors(cors -> cors.configurationSource(request -> {
	            CorsConfiguration config = new CorsConfiguration();
	            config.setAllowedOrigins(
	            		List.of("http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5173", "http://127.0.0.1:5174"));
	            config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
	            config.setAllowedHeaders(List.of("*"));
	            config.setAllowCredentials(true);
	            return config;
	        }))
	        //Disable CSRF
	        .csrf(csrf -> csrf.disable())
	        // OAuth2 authorization code flow requires HTTP session to store auth request state.
	        .sessionManagement(session -> session
	        	.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
	        	.sessionFixation(fix -> fix.migrateSession())
	        )
	        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
	        // FORM LOGIN
	        .formLogin(form -> form
	        	//.disable()
	            .loginPage("/")               
	            .loginProcessingUrl("/login/validate")     
	            .successHandler(customSuccessHandler)
	            .failureHandler(customFormLoginFailureHandler)
	            .usernameParameter("username")
	            .passwordParameter("pwd")
	        )
	      //OAuth2 Login
			.oauth2Login(oauth -> oauth
					.successHandler(oAuth2LoginSuccessHandler)
					.failureHandler(oAuth2LoginFailureHandler))
	        // EXCEPTION HANDLING
	        .exceptionHandling(ex -> ex
	            .authenticationEntryPoint(customAuthenticationEntryPoint
	            	/*
	            	(req, res, e) -> {
	            		res.sendRedirect("/");
	            	}
	            	*/
	            )
	            .accessDeniedHandler(customAccessDeniedHandler
	            	/*
	                (req, res, e) -> {
	                	res.sendRedirect("/");
	            	}
	            	*/
	            )
	        )
	        .rememberMe(reme -> reme
	         		.rememberMeParameter("remember-me")
	        		.rememberMeCookieName("remember-me")
	        		.tokenValiditySeconds(30*24*60*60)
	        );

	    return http.build();
	}
	
}
