package org.ipan.nrgyrent.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
//import org.springframework.security.authentication.AuthenticationEventPublisher;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
//import org.springframework.security.authentication.ProviderManager;
//import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.factory.PasswordEncoderFactories;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//@Configuration
/*public class SecurityConfig {
    @Bean
    @ConditionalOnMissingBean(AuthenticationEventPublisher.class)
    DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher(ApplicationEventPublisher delegate) {
        return new DefaultAuthenticationEventPublisher(delegate);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(authenticationProvider);
    }


*//*
    @Bean
    public SecurityFilterChain securityFilterChain(TelegramRequestFilter telegramRequestFilter, HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/auth/login").permitAll() // will be handled by the LoginController
                        .requestMatchers("/dictionary/*").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
*//*
*//*                        .requestMatchers("/", "/index.html", "/favicon.ico",
                                "/**.css", "/**.js", "/**.png", "/**.jpg",
                                "/**.jpeg", "/**.gif", "/**.svg", "/**.woff2",
                                "/**.ttf", "/**.eot").permitAll()*//**//*

                )
                .csrf(csrf -> csrf.disable());

        http.addFilterBefore(telegramRequestFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
*//*


    @Bean
    @Scope("session")
    public CurrentUserInfo currentUserInfo(CurrentUserProvider currentUserProvider) {
        return currentUserProvider.getCurrentUser();
    }
}*/
