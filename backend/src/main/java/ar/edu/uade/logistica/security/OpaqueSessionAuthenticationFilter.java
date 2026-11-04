package ar.edu.uade.logistica.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class OpaqueSessionAuthenticationFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer ";
    private final SessionAuthenticationService authenticationService;

    public OpaqueSessionAuthenticationFilter(SessionAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(PREFIX) && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticationService.authenticate(header.substring(PREFIX.length()).trim()).ifPresent(user -> {
                var authorities = new ArrayList<SimpleGrantedAuthority>();
                user.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
                user.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user, null, authorities));
            });
        }
        chain.doFilter(request, response);
    }
}

