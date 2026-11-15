package com.team.corporate.backend.config;

import com.team.corporate.backend.security.BackendPasswordEncoder;
import com.team.corporate.backend.security.CredentialUserDetailsService;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static org.springframework.http.HttpStatus.NO_CONTENT;

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
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            @NotNull HttpSecurity http,
            @NotNull DaoAuthenticationProvider provider,
            @NotNull SessionRegistry sessions,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver errors
    ) {
        http.authenticationProvider(provider)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                        .requestMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, _) -> errors.resolveException(
                                request, response, null, new InsufficientAuthenticationException("Требуется вход.")))
                        .accessDeniedHandler((request, response, exception) ->
                                errors.resolveException(request, response, null, exception)))
                .formLogin(login -> login
                        .loginPage("/api/auth/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((_, response, _) -> response.setStatus(NO_CONTENT.value()))
                        .failureHandler((request, response, exception) ->
                                errors.resolveException(request, response, null, exception))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(NO_CONTENT)))
                .sessionManagement(session -> session
                        .maximumSessions(-1)
                        .sessionRegistry(sessions)
                        .expiredSessionStrategy(event -> errors.resolveException(
                                event.getRequest(), event.getResponse(), null,
                                new CredentialsExpiredException("Сессия завершена. Войдите повторно."))));
        return http.build();
    }
}
