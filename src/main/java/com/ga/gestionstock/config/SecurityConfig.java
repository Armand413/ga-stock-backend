package com.ga.gestionstock.config;

import com.ga.gestionstock.service.AuthService;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    // L'authentification est geree par AuthService ; pas de compte genere par Spring.
    @Bean UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("Utiliser /api/auth/login"); };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthService auth,
                                            @org.springframework.beans.factory.annotation.Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(cors))
                .httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").authenticated()
                        .requestMatchers("/api/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/catalogue", "/api/mes-demandes", "/api/mes-consommations").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/demandes").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/demandes/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/demandes/{id}/annulation").authenticated()
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setHeader("WWW-Authenticate", "Bearer");
                            res.setContentType("application/problem+json;charset=UTF-8");
                            res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Authentification requise\",\"status\":401,\"detail\":\"Connectez-vous avec un jeton valide.\"}");
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/problem+json;charset=UTF-8");
                            res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Acces refuse\",\"status\":403,\"detail\":\"Droits insuffisants.\"}");
                        }))
                .addFilterBefore(new BearerTokenFilter(auth), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins:http://localhost:4200}") String origins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cors.setExposedHeaders(List.of("Location", "Content-Disposition"));
        cors.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
