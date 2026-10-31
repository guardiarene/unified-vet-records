package tfg.psygcv.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tfg.psygcv.support.AuthTestFactory;
import tfg.psygcv.user.entity.Role;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class CustomAuthenticationSuccessHandlerTest {

  @Test
  void should_logUserIdAndRole_andRedirect_when_AuthenticatedUser(CapturedOutput output)
      throws Exception {
    CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    var auth = AuthTestFactory.authentication(AuthTestFactory.user(7L, Role.CUSTOMER));

    handler.onAuthenticationSuccess(request, response, auth);

    assertThat(response.getRedirectedUrl()).isEqualTo("/customer/dashboard");
    assertThat(output).contains("userId=7").contains("role=ROLE_CUSTOMER");
    assertThat(output).doesNotContain("@test.com");
  }

  @Test
  void should_redirect_when_nonAuthenticatedPrincipal(CapturedOutput output) throws Exception {
    CustomAuthenticationSuccessHandler handler = new CustomAuthenticationSuccessHandler();
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    var auth =
        new UsernamePasswordAuthenticationToken(
            "anonymous", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

    handler.onAuthenticationSuccess(request, response, auth);

    assertThat(response.getRedirectedUrl()).isEqualTo("/customer/dashboard");
  }
}
