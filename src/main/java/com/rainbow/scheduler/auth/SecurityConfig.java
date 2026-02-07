package com.rainbow.scheduler.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/css/**", "/js/**", "/login.html").permitAll() // Allow
                                                                                                                 // static
                                                                                                                 // assets
                                                                                                                 // &
                                                                                                                 // login
                                                                                                                 // page
                                                .anyRequest().authenticated() // Secure everything else
                                )
                                .formLogin(form -> form
                                                .loginPage("/login.html")
                                                .loginProcessingUrl("/perform_login")
                                                .defaultSuccessUrl("/", true)
                                                .permitAll())
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/login.html")
                                                .defaultSuccessUrl("/", true))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login.html?logout")
                                                .permitAll());

                return http.build();
        }

        @Bean
        public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
                org.springframework.security.core.userdetails.UserDetails user = org.springframework.security.core.userdetails.User
                                .withDefaultPasswordEncoder()
                                .username("aman")
                                .password("password")
                                .roles("USER")
                                .build();
                return new org.springframework.security.provisioning.InMemoryUserDetailsManager(user);
        }
}
