package tfg.psygcv.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tfg.psygcv.shared.logging.LogSanitizer;

@Slf4j
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

  public CustomAuthenticationFailureHandler() {
    setDefaultFailureUrl("/users/login?error=true");
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, jakarta.servlet.ServletException {
    String email = request.getParameter("username");
    log.warn(
        "Failed login attempt [email={}, reason={}]",
        LogSanitizer.maskEmail(email),
        exception.getClass().getSimpleName());
    super.onAuthenticationFailure(request, response, exception);
  }
}
