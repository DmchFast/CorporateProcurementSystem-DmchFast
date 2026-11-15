package com.team.corporate.backend.config;

import com.team.corporate.backend.security.BackendPasswordEncoder;
import com.team.corporate.backend.security.CredentialUserDetailsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import com.team.corporate.backend.security.SessionJwtAuthenticationConverter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BackendPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            @NotNull CredentialUserDetailsService users,
            @NotNull PasswordEncoder encoder
    ) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(@NotNull DaoAuthenticationProvider provider) {
        return new ProviderManager(provider);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authenticationEndpoints(@NotNull HttpSecurity http) {
        return stateless(http)
                .securityMatcher("/api/auth/login", "/api/auth/refresh", "/api/auth/logout")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            @NotNull HttpSecurity http,
            @NotNull SessionJwtAuthenticationConverter converter,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors
    ) {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            if (!(exception instanceof AuthenticationServiceException)) {
                response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            }
            errors.resolveException(request, response, null, exception);
        };
        return stateless(http)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, exception) ->
                                errors.resolveException(request, response, null, exception)))
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, exception) ->
                                errors.resolveException(request, response, null, exception)))
                .build();
    }

    private static HttpSecurity stateless(HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
