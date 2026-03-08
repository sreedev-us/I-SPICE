package com.company.I_SPICE.config;

import com.company.I_SPICE.services.CustomUserDetailsService;
import com.company.I_SPICE.services.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

        @Value("${SECURITY_REMEMBER_ME_KEY:uniqueAndSecretKeyForI-SPICE}")
        private String rememberMeKey;

        public WebSecurityConfig(CustomUserDetailsService userDetailsService,
                        CustomOAuth2UserService customOAuth2UserService,
                        OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
                this.userDetailsService = userDetailsService;
                this.customOAuth2UserService = customOAuth2UserService;
                this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName("_csrf");

                http
                                // Enable CSRF
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(requestHandler)
                                                .ignoringRequestMatchers("/api/public/**") // Allow some public APIs
                                )
                                .headers(headers -> headers
                                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net https://checkout.razorpay.com; "
                                                                                +
                                                                                "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com/ajax/libs; "
                                                                                +
                                                                                "font-src 'self' data: https://cdnjs.cloudflare.com/ajax/libs https://r2cdn.perplexity.ai; "
                                                                                +
                                                                                "img-src 'self' data: https: http:; " +
                                                                                "connect-src 'self' https://*.supabase.co https://api.razorpay.com; "
                                                                                +
                                                                                "frame-src 'self' https://api.razorpay.com;")))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints
                                                .requestMatchers(
                                                                "/",
                                                                "/home",
                                                                "/dashboard",
                                                                "/register",
                                                                "/login",
                                                                "/logout",
                                                                "/css/**",
                                                                "/js/**",
                                                                "/images/**",
                                                                "/fontawesome/**",
                                                                "/I-SPICE/**",
                                                                "/favicon.ico",
                                                                "/error",
                                                                // Product pages (public for viewing)
                                                                "/products",
                                                                "/products/**",
                                                                "/product",
                                                                "/product/**",
                                                                "/shop",
                                                                "/shop/**",
                                                                "/categories",
                                                                "/categories/**",
                                                                // API endpoints
                                                                "/api/public/**",
                                                                "/api/register",
                                                                "/api/login",
                                                                "/api/products/**",
                                                                // Email verification
                                                                "/verify-email",
                                                                "/verify-email/**",
                                                                "/forgot-password",
                                                                "/reset-password",
                                                                "/reset-password/**",
                                                                // API endpoints
                                                                "/api/reviews/**", // Public viewing of reviews
                                                                // Static resources
                                                                "/webjars/**",
                                                                "/actuator/health")
                                                .permitAll()
                                                // Protected endpoints (require login for actions)
                                                .requestMatchers(
                                                                "/cart/**",
                                                                "/checkout/**",
                                                                "/profile",
                                                                "/profile/**",
                                                                "/orders",
                                                                "/orders/**",
                                                                "/wishlist",
                                                                "/wishlist/**",
                                                                "/subscriptions",
                                                                "/support", "/support/ticket", "/support/ticket/**",
                                                                "/api/cart/**",
                                                                "/api/orders/**",
                                                                "/api/wishlist/**",
                                                                "/api/profile/**",
                                                                "/api/support/**",
                                                                "/api/reviews/submit")
                                                .authenticated()
                                                // Admin endpoints
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                // All other requests
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/home", true)
                                                .failureUrl("/login?error=true")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login")
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oAuth2LoginSuccessHandler)
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .permitAll())
                                .rememberMe(remember -> remember
                                                .key(rememberMeKey)
                                                .tokenValiditySeconds(7 * 24 * 60 * 60)
                                                .rememberMeParameter("remember-me")
                                                .userDetailsService(userDetailsService)
                                                .alwaysRemember(false))
                                // Session management
                                .sessionManagement(session -> session
                                                .sessionFixation().migrateSession()
                                                .maximumSessions(1)
                                                .maxSessionsPreventsLogin(false));

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
                AuthenticationManagerBuilder authenticationManagerBuilder = http
                                .getSharedObject(AuthenticationManagerBuilder.class);
                authenticationManagerBuilder
                                .userDetailsService(userDetailsService)
                                .passwordEncoder(passwordEncoder());
                return authenticationManagerBuilder.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
