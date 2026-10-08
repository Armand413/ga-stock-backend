package com.ga.gestionstock.config;

import com.ga.gestionstock.service.AuthService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class BearerTokenFilter extends OncePerRequestFilter {
    private final AuthService auth;
    public BearerTokenFilter(AuthService auth) { this.auth = auth; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            auth.authentifier(header.substring(7)).ifPresent(user -> {
                var authorities = new java.util.ArrayList<SimpleGrantedAuthority>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
                user.permissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.name())));
                var authentication = new UsernamePasswordAuthenticationToken(user, null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}

