package com.mecky.attendance.config;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
@Configuration @EnableMethodSecurity public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain filterChain(HttpSecurity http,JwtFilter jwt)throws Exception{
  return http.csrf(c->c.disable()).authorizeHttpRequests(a->a
   .requestMatchers("/", "/index.html", "/favicon.ico", "/icon-192.png", "/*.styles.css", "/css/**", "/lib/**", "/_framework/**", "/_content/**").permitAll()
   .requestMatchers("/api/auth/login","/api/device/**").permitAll()
   .requestMatchers("/api/employee-portal/**").hasRole("EMPLOYEE")
   .requestMatchers("/ws").hasAnyRole("ADMIN","EMPLOYEE")
   .anyRequest().hasRole("ADMIN"))
   .addFilterBefore(jwt,UsernamePasswordAuthenticationFilter.class).build();
 }
}
