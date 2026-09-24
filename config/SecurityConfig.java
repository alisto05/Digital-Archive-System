package com.syncpoint.archive.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;


@Configuration
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                          PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                      SecurityContextRepository securityContextRepository)
            throws Exception {

        http
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true)
                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                       
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login/patient", "/api/auth/login/staff", "/api/auth/login/admin",
                                "/api/patients/register"
                        ).permitAll()

                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()

                       
                        .requestMatchers(HttpMethod.GET,
                                "/api/patients/*/profile",
                                "/api/documents/patient/*",
                                "/api/documents/types",
                                "/api/document-requests/patient/*"
                        ).hasAnyRole("PATIENT", "STAFF", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/documents/upload")
                        .hasAnyRole("PATIENT", "STAFF", "ADMIN")

                       
                        .requestMatchers(
                                "/api/approvals/**",
                                "/api/documents/staff-search",
                                "/api/documents/pending-review",
                                "/api/dashboard/**",
                                "/api/document-requests"
                        ).hasAnyRole("STAFF", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/documents/*/review")
                        .hasAnyRole("STAFF", "ADMIN")

                       
                        .requestMatchers("/api/staff/register", "/api/admin/register")
                        .hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403))
                );

        return http.build();
    }
}
