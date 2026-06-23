package poly.edu.ASSM.Services.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.ASSM.security.SpringRoleNames;
import poly.edu.ASSM.Services.core.UserDetailsServiceImpl;

@Service
public class JwtAuthFilter extends OncePerRequestFilter {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserDetailsServiceImpl userService;

	private static List<String> toRoleStringList(Object raw) {
		if (raw == null) {
			return new ArrayList<>();
		}
		if (!(raw instanceof Collection<?> col)) {
			return new ArrayList<>();
		}
		List<String> out = new ArrayList<>();
		for (Object o : col) {
			if (o != null) {
				out.add(o.toString());
			}
		}
		return out;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.startsWith("Bearer ")) {
			String token = authorization.substring(7).trim();
			try {
				Claims claims = jwtService.getBody(token);
				if (jwtService.validate(claims)) {
					String username = claims.getSubject();
					List<String> roles = toRoleStringList(claims.get("roles"));
					List<String> permissions = toRoleStringList(claims.get("permissions"));
					List<SimpleGrantedAuthority> authorities;
					if (roles.isEmpty() && username != null && !username.isBlank()) {
						try {
							UserDetails user = userService.loadUserByUsername(username);
							authorities = user.getAuthorities().stream()
									.map(a -> new SimpleGrantedAuthority(
											a.getAuthority().startsWith("ROLE_")
													? SpringRoleNames.normalize(a.getAuthority())
													: a.getAuthority()))
									.filter(a -> !a.getAuthority().isEmpty())
									.toList();
						} catch (UsernameNotFoundException ex) {
							authorities = List.of();
						}
					} else {
						authorities = new ArrayList<>();
						roles.stream()
								.map(SpringRoleNames::normalize)
								.filter(s -> !s.isEmpty())
								.map(SimpleGrantedAuthority::new)
								.forEach(authorities::add);
						permissions.stream()
								.filter(s -> s != null && !s.isBlank())
								.map(SimpleGrantedAuthority::new)
								.forEach(authorities::add);
					}
					var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(auth);
				}
			} catch (Exception e) {
				// Malformed/expired token, bad signature, etc. — continue as anonymous.
				SecurityContextHolder.clearContext();
			}
		}

		filterChain.doFilter(request, response);
	}
}
