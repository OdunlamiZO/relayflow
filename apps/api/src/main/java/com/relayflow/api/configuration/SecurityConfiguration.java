package com.relayflow.api.configuration;

import com.relayflow.api.authentication.EmailPasswordUserDetailsService;
import com.relayflow.api.authentication.OAuth2UserProvisioningService;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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

    @Bean
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
                                                "/api/auth/login",
                                                "/api/auth/guest",
                                                "/api/telegram/webhook/**",
                                                "/api/telegram/webhook/shared",
                                                "/oauth2/authorization/**",
                                                "/login/oauth2/code/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
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
