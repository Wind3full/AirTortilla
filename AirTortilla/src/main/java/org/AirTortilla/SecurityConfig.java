package org.AirTortilla;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    @SuppressWarnings("deprecation")
    PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository users) {
        return username -> {
            AppUser user = users.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException(
                            "Usuario no encontrado"
                    ));

            return org.springframework.security.core.userdetails.User
                    .withUsername(user.getUsername())
                    .password(user.getPassword())
                    .roles(user.getRole().name())
                    .build();
        };
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/inicio.html",
                                "/reservas.html",
                                "/consultar.html",
                                "/csv.html",
                                "/nueva-reserva.html",
                                "/admin.html",
                                "/app.js",
                                "/styles.css",
                                "/api/auth/login",
                                "/error"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users")
                        .hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/api/audit")
                        .hasRole("ADMINISTRADOR")
                        .anyRequest()
                        .authenticated()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .build();
    }
}
