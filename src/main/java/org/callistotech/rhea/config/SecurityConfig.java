package org.callistotech.rhea.config;

import org.callistotech.rhea.model.AppUser;
import org.callistotech.rhea.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

/**
 * User login: standalone email/password sign-up plus Google OAuth2, backed by the {@code rhea_users}
 * table ({@link UserService} / {@link org.callistotech.rhea.repository.UserRepository}).
 *
 * The login mechanism stays wired up regardless of {@code REQUIRE_AUTH} -- /login still works --
 * but when {@code REQUIRE_AUTH=false} the /api/** matcher is left out of the authenticated rule,
 * so unauthenticated demo traffic passes through. Defaults to true (enforced) everywhere except
 * where explicitly overridden, e.g. the local demo launch config.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${rhea.require-auth:true}") boolean requireAuth) throws Exception {

        if (!requireAuth) {
            http
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
            return http.build();
        }

        http
                // Spring Security 6 defaults requireExplicitSave=true, which can silently drop
                // the session after OAuth2 login when custom success handlers are in use.
                .securityContext(ctx -> ctx
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/login", "/login.html",
                                "/api/auth/register",
                                "/static/**", "/css/**", "/js/**",
                                "/callisto_high.png", "/favicon.ico",
                                "/actuator/health", "/error")
                        .permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/insurance-programs", "/api/config")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(jsonSuccessHandler())
                        .failureHandler((req, res, ex) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Invalid email or password.\"}");
                        })
                        .permitAll())
                .oauth2Login(oauth -> oauth
                        .loginPage("/login")
                        .authorizationEndpoint(ep -> ep
                                .authorizationRequestRepository(new CookieOAuth2AuthorizationRequestRepository()))
                        .successHandler(oAuth2SuccessHandler())
                        .failureHandler(oAuth2FailureHandler()))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> {
                            res.setStatus(HttpServletResponse.SC_OK);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"loggedOut\":true}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authEntryPoint()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    private AuthenticationEntryPoint authEntryPoint() {
        return (request, response, authException) -> {
            String xhr = request.getHeader("X-Requested-With");
            String accept = request.getHeader("Accept");
            boolean isApi = request.getRequestURI().startsWith("/api/")
                    || "XMLHttpRequest".equals(xhr)
                    || (accept != null && accept.contains("application/json"));
            if (isApi) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"loggedIn\":false}");
            } else {
                response.sendRedirect("/login");
            }
        };
    }

    private AuthenticationSuccessHandler jsonSuccessHandler() {
        return (request, response, authentication) -> {
            String userName = authentication.getName();
            AppUser user = userService.findByUserName(userName).orElse(null);
            String name = user != null && user.getName() != null ? user.getName() : userName;
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"loggedIn\":true,\"email\":\"%s\",\"name\":\"%s\"}", userName, name));
        };
    }

    private AuthenticationFailureHandler oAuth2FailureHandler() {
        return (request, response, ex) -> response.sendRedirect("/login?error=google");
    }

    private AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            userService.findOrCreateGoogleUser(oAuth2User);
            response.sendRedirect("/");
        };
    }
}
