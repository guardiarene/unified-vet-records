package tfg.psygcv.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Authentication authentication)
      throws IOException {
    String role =
        authentication.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElseThrow(
                () -> new IllegalStateException("Authenticated dashboard has no authorities"));
    if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
      log.info("Successful login [userId={}, role={}]", authenticatedUser.getId(), role);
    } else {
      log.info("Successful login [role={}]", role);
    }
    String redirectUrl =
        switch (role) {
          case "ROLE_VETERINARIAN" -> "/veterinarian/dashboard";
          case "ROLE_RECEPTIONIST" -> "/receptionist/dashboard";
          case "ROLE_CUSTOMER" -> "/customer/dashboard";
          case "ROLE_SYSTEM_ADMINISTRATOR" -> "/admin/dashboard";
          default -> throw new IllegalStateException("Non-existent role: " + role);
        };
    response.sendRedirect(redirectUrl);
  }
}
