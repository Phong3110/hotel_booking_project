package com.example.HotelBooking.security;

import com.example.HotelBooking.exceptions.CustomAccessDenialHandler;
import com.example.HotelBooking.exceptions.CustomAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityFilter {

    private final AuthFilter authFilter;
    private final CustomAccessDenialHandler customAccessDenialHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception ->
                        exception.accessDeniedHandler(customAccessDenialHandler)
                                .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(request -> request
                        // 1. PUBLIC ENDPOINTS - Không cần authentication
                        .requestMatchers(
                                "/api/auth/**",           // Login, Register
                                "/api/rooms/all",         // Danh sách phòng (public)
                                "/api/rooms/types",       // Loại phòng (public)
                                "/api/rooms/available",   // Search phòng available (public)
                                "/api/rooms/{id}",        // Chi tiết phòng (public)
                                "/api/bookings/status",   // Check payment status với token (public)
                                "/api-docs/**",           // Swagger docs
                                "/swagger-ui/**",         // Swagger UI
                                "/v3/api-docs/**",        // OpenAPI docs
                                "/rooms/**"               // Images
                        ).permitAll()

                        // 2. ADMIN ONLY ENDPOINTS
                        .requestMatchers(
                                "/api/rooms/add",         // Thêm phòng
                                "/api/rooms/update",      // Sửa phòng
                                "/api/rooms/delete/**",   // Xóa phòng
                                "/api/bookings/all",      // Xem tất cả bookings
                                "/api/bookings/update",   // Update booking status
                                "/api/users/all"          // Xem tất cả users
                        ).hasAuthority("ADMIN")

                        // 3. AUTHENTICATED USERS (ADMIN + CUSTOMER)
                        .requestMatchers(
                                "/api/bookings",          // POST - Tạo booking mới
                                "/api/bookings/{reference}", // GET - Xem booking của mình
                                "/api/bookings/cancel/**", // DELETE - Hủy booking của mình
                                "/api/users/**",          // User profile endpoints
                                "/api/stripe/**",         // Stripe payment
                                "/api/paypal/**"          // PayPal payment
                        ).authenticated()

                        // 4. Tất cả requests khác đều cần authenticated
                        .anyRequest().authenticated()
                )
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
