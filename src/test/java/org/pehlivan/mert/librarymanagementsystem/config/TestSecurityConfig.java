package org.pehlivan.mert.librarymanagementsystem.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.pehlivan.mert.librarymanagementsystem.security.JwtAuthFilter;
import org.pehlivan.mert.librarymanagementsystem.security.JwtHelper;
import org.pehlivan.mert.librarymanagementsystem.service.user.CustomUserDetailsService;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtHelper jwtHelper, CustomUserDetailsService userDetailsService) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests()
                .requestMatchers("/api/v1/authors").hasRole("LIBRARIAN")
                .requestMatchers("/api/v1/authors/**").hasAnyRole("LIBRARIAN", "READER")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthFilter(jwtHelper, userDetailsService), UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
} 