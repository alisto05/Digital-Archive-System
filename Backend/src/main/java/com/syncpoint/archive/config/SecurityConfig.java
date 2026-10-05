package com.syncpoint.archive.config;

import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.security.AppUserDetails;
import com.syncpoint.archive.security.JsonErrors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import jakarta.servlet.DispatcherType;

import java.util.List;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(),
                new CsrfAuthenticationStrategy(csrfTokenRepository)
        ));
    }

  
    @Bean
    public LogoutHandler loginHistoryLogoutHandler(AuthDao authDao) {
        return (request, response, authentication) -> {
            if (authentication != null
                    && authentication.getPrincipal() instanceof AppUserDetails user) {
                try {
                    authDao.recordLogout(user.loginId());
                } catch (DataAccessException ignored) {
                   
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            LogoutHandler loginHistoryLogoutHandler,
            AuthDao authDao)
            throws Exception {

        http
                .securityContext(context -> context
                        .securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .sessionManagement(session -> session
                        .sessionAuthenticationStrategy(sessionAuthenticationStrategy))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .authorizeHttpRequests(authorize -> authorize

                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()

                       
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login/patient",
                                "/api/auth/login/staff",
                                "/api/auth/login/admin",
                                "/api/patients/register").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/status",
                                "/api/auth/csrf").permitAll()

                        
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/documents/types").authenticated()

                       
                        .requestMatchers(HttpMethod.POST, "/api/staff/register").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/staff/me").hasRole("STAFF")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                       
                        .requestMatchers(HttpMethod.GET, "/api/dashboard/staff-stats")
                                .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/approvals/pending-patients").hasRole("STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/patients/search").hasRole("STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/approvals/patients/*/status").hasRole("STAFF")
                        
                        .requestMatchers(HttpMethod.GET, "/api/documents/staff-search")
                                .hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/documents/pending-review").hasRole("STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/documents/*/review").hasRole("STAFF")
                        .requestMatchers(HttpMethod.POST, "/api/document-requests").hasRole("STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/profile-change-requests/pending").hasRole("STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/profile-change-requests/*/resolve").hasRole("STAFF")

                        
                        .requestMatchers(HttpMethod.POST, "/api/profile-change-requests").hasRole("PATIENT")

                      
                        .requestMatchers(HttpMethod.GET, "/api/approvals/patients/*/recent-activity")
                                .hasAnyRole("PATIENT", "STAFF")
                       
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/download")
                                .hasAnyRole("PATIENT", "STAFF", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/documents/upload")
                                .hasAnyRole("PATIENT", "STAFF")
                        .requestMatchers(HttpMethod.GET,
                                "/api/documents/patient/*",
                                "/api/profile-change-requests/patient/*",
                                "/api/document-requests/patient/*",
                                "/api/patients/*/profile").hasAnyRole("PATIENT", "STAFF")

                      
                        .anyRequest().denyAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .addLogoutHandler(loginHistoryLogoutHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                JsonErrors.write(response, 401, "Authentication is required."))
                        .accessDeniedHandler((request, response, exception) ->
                                JsonErrors.write(response, 403,
                                        exception instanceof CsrfException
                                                ? "Missing or invalid CSRF token."
                                                : "You do not have permission to do this.")));

        http.addFilterBefore(new UserSessionValidationFilter(authDao), AuthorizationFilter.class);
        return http.build();
    }
}
