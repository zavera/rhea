package org.callistotech.rhea.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * MVP auth: bcrypt-backed local pharmacy-staff login, credentials from env vars.
 * Google OAuth2 (per brand system) is a follow-up once client credentials are provisioned.
 *
 * The login mechanism stays wired up regardless of {@code REQUIRE_AUTH} -- /login still
 * works -- but when {@code REQUIRE_AUTH=false} the /api/** matcher is left out of the
 * authenticated rule, so unauthenticated demo traffic passes through. Defaults to true
 * (enforced) everywhere except where explicitly overridden, e.g. the local demo launch
 * config.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder encoder,
            @Value("${rhea.pharmacy-user:pharmacist}") String username,
            @Value("${rhea.pharmacy-password:changeme-local-dev}") String rawPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(encoder.encode(rawPassword))
                        .roles("PHARMACY_STAFF")
                        .build());
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Value("${rhea.require-auth:true}") boolean requireAuth) throws Exception {
        http
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**",
                            "/favicon.ico", "/actuator/health").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/insurance-programs", "/api/config").permitAll();
                    if (requireAuth) {
                        auth.requestMatchers("/api/**").authenticated();
                    }
                    auth.anyRequest().permitAll();
                })
                .httpBasic(basic -> {})
                .formLogin(form -> form.defaultSuccessUrl("/", true))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));
        return http.build();
    }
}
