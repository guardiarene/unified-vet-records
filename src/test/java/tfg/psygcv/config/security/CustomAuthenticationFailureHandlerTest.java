package tfg.psygcv.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CustomAuthenticationFailureHandlerTest {

  @Test
  void should_redirectAndLogMaskedEmail(CapturedOutput output)
      throws IOException, ServletException {
    CustomAuthenticationFailureHandler handler = new CustomAuthenticationFailureHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("username", "user@test.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

    assertThat(response.getRedirectedUrl()).isEqualTo("/users/login?error=true");
    assertThat(output).contains("u***@test.com").doesNotContain("user@test.com");
  }

  @Test
  void should_produceSingleLogLine_when_emailHasCrLf(CapturedOutput output)
      throws IOException, ServletException {
    CustomAuthenticationFailureHandler handler = new CustomAuthenticationFailureHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setParameter("username", "user\r\ntest@test.com");
    MockHttpServletResponse response = new MockHttpServletResponse();

    handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad"));

    long lines = output.toString().lines().filter(l -> l.contains("Failed login attempt")).count();
    assertThat(lines).isEqualTo(1);
    assertThat(output).doesNotContain("test@test.com");
  }
}
