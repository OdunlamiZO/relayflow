package com.relayflow.api.configuration;

import com.relayflow.api.authentication.ApiKeyAuthenticationFilter;
import com.relayflow.api.authentication.EmailPasswordUserDetailsService;
import com.relayflow.api.authentication.OAuth2UserProvisioningService;
import com.relayflow.api.messaging.repository.WorkspaceApiKeyRepository;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfiguration {

    private final OAuth2UserProvisioningService oauth2UserProvisioningService;

    private final EmailPasswordUserDetailsService emailPasswordUserDetailsService;

    public SecurityConfiguration(
            OAuth2UserProvisioningService oauth2UserProvisioningService,
            EmailPasswordUserDetailsService emailPasswordUserDetailsService) {
        this.oauth2UserProvisioningService = oauth2UserProvisioningService;
        this.emailPasswordUserDetailsService = emailPasswordUserDetailsService;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(emailPasswordUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());

        return new ProviderManager(provider);
    }

    @Bean
    HttpSessionSecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /**
     * Stateless filter chain for the public API ({@code /public/v1/**}). Authenticated exclusively
     * via the {@code X-Api-Key} header — no session, no CSRF.
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicApiFilterChain(
            HttpSecurity http, WorkspaceApiKeyRepository apiKeyRepository) throws Exception {
        ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(apiKeyRepository);

        http.securityMatcher("/public/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${relayflow.auth.google.enabled:false}") boolean googleAuthEnabled,
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl)
            throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth ->
                                auth.dispatcherTypeMatchers(
                                                DispatcherType.ASYNC, DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/health",
                                                "/api/auth/me",
                                                "/api/auth/signup",
                                                "/api/auth/verify-email",
                                                "/api/auth/login",
                                                "/api/auth/login/2fa",
                                                "/api/auth/guest",
                                                "/api/telegram/webhook/**",
                                                "/api/telegram/webhook/shared",
                                                "/api/whatsapp/webhook/**",
                                                "/oauth2/authorization/**",
                                                "/login/oauth2/code/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        // Invite preview is public — anyone with the link can see
                                        // workspace name + inviter before deciding to sign up.
                                        .requestMatchers(HttpMethod.GET, "/api/invites/*")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .httpBasic(Customizer.withDefaults());

        if (googleAuthEnabled) {
            http.oauth2Login(
                    oauth2 ->
                            oauth2.userInfoEndpoint(
                                            userInfo ->
                                                    userInfo.userService(
                                                            oauth2UserProvisioningService))
                                    .defaultSuccessUrl(webBaseUrl, true));
        }

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${relayflow.web.base-url:http://localhost:3000}") String webBaseUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(webBaseUrl));
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
