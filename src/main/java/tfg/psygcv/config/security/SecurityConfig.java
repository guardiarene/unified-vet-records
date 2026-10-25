package tfg.psygcv.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
    httpSecurity
        .csrf(Customizer.withDefaults())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/users/login",
                        "/users/register",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/error",
                        "/access_denied")
                    .permitAll()
                    .requestMatchers("/users/new", "/users/edit/**", "/users/delete/**")
                    .hasRole("SYSTEM_ADMINISTRATOR")
                    .requestMatchers("/admin/**")
                    .hasRole("SYSTEM_ADMINISTRATOR")
                    .requestMatchers("/customer/**")
                    .hasRole("CUSTOMER")
                    .requestMatchers("/clinics/register")
                    .permitAll()
                    .requestMatchers("/profile/**", "/pets/**", "/clinics/**")
                    .hasAnyRole("CUSTOMER", "VETERINARIAN", "RECEPTIONIST")
                    .requestMatchers("/medical-services/**")
                    .hasRole("VETERINARIAN")
                    .requestMatchers("/medical-records/new", "/medical-records/*/edit")
                    .hasRole("VETERINARIAN")
                    .requestMatchers("/medical-records/**")
                    .hasAnyRole("CUSTOMER", "VETERINARIAN")
                    .requestMatchers("/visits/new", "/visits/*/edit", "/visits/*/delete")
                    .hasRole("VETERINARIAN")
                    .requestMatchers("/visits/**")
                    .hasAnyRole("CUSTOMER", "VETERINARIAN")
                    .requestMatchers("/reports/diseases-treatments")
                    .hasRole("VETERINARIAN")
                    .requestMatchers("/reports/**")
                    .hasAnyRole("RECEPTIONIST", "VETERINARIAN")
                    .requestMatchers(
                        "/appointments/schedule",
                        "/appointments/*/reschedule",
                        "/appointments/*/update-status")
                    .hasRole("RECEPTIONIST")
                    .requestMatchers("/appointments/new")
                    .hasRole("CUSTOMER")
                    .requestMatchers("/appointments/**")
                    .hasAnyRole("CUSTOMER", "RECEPTIONIST")
                    .requestMatchers("/receptionist/**")
                    .hasRole("RECEPTIONIST")
                    .requestMatchers("/veterinarian/**")
                    .hasRole("VETERINARIAN")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(exceptions -> exceptions.accessDeniedPage("/access_denied"))
        .formLogin(
            form ->
                form.loginPage("/users/login")
                    .successHandler(customAuthenticationSuccessHandler)
                    .failureUrl("/users/login?error=true")
                    .permitAll())
        .logout(
            logout ->
                logout.logoutUrl("/logout").logoutSuccessUrl("/users/login?logout").permitAll());
    return httpSecurity.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
