package com.example.techjobs.config;
import com.example.techjobs.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
@Configuration
public class SecurityConfig {
  @Bean PasswordEncoder encoder(){ return new BCryptPasswordEncoder(); }
  @Bean AuthenticationManager authManager(AuthenticationConfiguration c) throws Exception { return c.getAuthenticationManager(); }
  @Bean UserDetailsService uds(UserRepository r){
    return email->r.findByEmail(email).map(u->User.withUsername(u.email).password(u.password).roles(u.role).build())
      .orElseThrow(()->new UsernameNotFoundException("not found")); }

  @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
    http.csrf(c->c.disable()).cors(Customizer.withDefaults())
      .authorizeHttpRequests(a->a
        .requestMatchers("/api/auth/register","/api/auth/login").permitAll()
        .requestMatchers("/api/admin/**","/api/jobs/sync").hasRole("ADMIN")
        .anyRequest().authenticated())
      .exceptionHandling(e->e
        .authenticationEntryPoint((q,s,x)->json(s,401,"Authentication required"))
        .accessDeniedHandler((q,s,x)->json(s,403,"Admin access required")));
    return http.build(); }

  private static void json(HttpServletResponse s,int code,String msg) throws IOException {
    s.setStatus(code); s.setContentType("application/json");
    s.getWriter().write("{\"status\":"+code+",\"message\":\""+msg+"\",\"timestamp\":\""+LocalDateTime.now()+"\"}"); }

  @Bean CorsConfigurationSource corsConfigurationSource(@Value("${cors.origins}") List<String> origins){
    CorsConfiguration c=new CorsConfiguration();
    c.setAllowedOrigins(origins); c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    c.setAllowedHeaders(List.of("*")); c.setAllowCredentials(true);   // lets the browser send the session cookie
    UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/api/**",c); return s; }
}
