package tfg.psygcv;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tfg.psygcv.support.TestcontainersConfiguration;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;
import tfg.psygcv.user.repository.UserRepository;
import tfg.psygcv.user.service.UserService;

/**
 * Verifies the Actuator exposure and URL security from the outside, with the JDK HTTP client.
 *
 * <p>Uses the same context configuration as {@link PSyGCVApplicationTests} so both classes share
 * the cached context and one MySQL Testcontainers instance.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ActuatorHealthTest {

  private static final String HEALTH_PATH = "/actuator/health";
  private static final String ENV_PATH = "/actuator/env";
  private static final String PROTECTED_PATH = "/pets";
  private static final String LOGIN_PATH = "/users/login";
  private static final String ADMIN_EMAIL = "actuator-health@test.com";
  private static final String ADMIN_PASSWORD = "ActuatorHealth123";
  private static final Pattern CSRF_TOKEN =
      Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

  @LocalServerPort
  private int port;

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  @Test
  void should_returnStatusUpWithoutDetails_when_healthRequestedWithoutAuthentication()
      throws Exception {
    HttpResponse<String> response = get(anonymousClient(), HEALTH_PATH);

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("\"status\":\"UP\"");
    assertThat(response.body()).doesNotContain("components");
    assertThat(response.body()).doesNotContain("details");
  }

  @Test
  void should_redirectToLogin_when_actuatorEnvRequestedWithoutAuthentication() throws Exception {
    HttpResponse<String> response = get(anonymousClient(), ENV_PATH);

    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().firstValue("Location").orElse("")).contains(LOGIN_PATH);
  }

  @Test
  void should_redirectToLogin_when_protectedRouteRequestedWithoutAuthentication() throws Exception {
    HttpResponse<String> response = get(anonymousClient(), PROTECTED_PATH);

    assertThat(response.statusCode()).isEqualTo(302);
    assertThat(response.headers().firstValue("Location").orElse("")).contains(LOGIN_PATH);
  }

  @Test
  void should_returnForbidden_when_administratorRequestsActuatorEnv() throws Exception {
    createAdministrator();
    HttpClient client = loginClient();

    HttpResponse<String> loginPage = get(client, LOGIN_PATH);
    String csrfToken = extractCsrfToken(loginPage.body());
    HttpResponse<String> login = postLogin(client, csrfToken);
    assertThat(login.statusCode()).isEqualTo(302);

    HttpResponse<String> response = get(client, ENV_PATH);

    assertThat(response.statusCode()).isEqualTo(403);
  }

  private HttpClient anonymousClient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
  }

  private HttpClient loginClient() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager())
        .followRedirects(HttpClient.Redirect.NEVER)
        .build();
  }

  private HttpResponse<String> get(HttpClient client, String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> postLogin(HttpClient client, String csrfToken) throws Exception {
    String body =
        "username="
            + encode(ADMIN_EMAIL)
            + "&password="
            + encode(ADMIN_PASSWORD)
            + "&_csrf="
            + encode(csrfToken);
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + LOGIN_PATH))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String extractCsrfToken(String html) {
    Matcher matcher = CSRF_TOKEN.matcher(html);
    if (!matcher.find()) {
      throw new IllegalStateException("CSRF token not found in the login page");
    }
    return matcher.group(1);
  }

  private void createAdministrator() {
    if (userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL).isPresent()) {
      return;
    }
    User admin = new User();
    admin.setEmail(ADMIN_EMAIL);
    admin.setPassword(ADMIN_PASSWORD);
    admin.setFirstName("Actuator");
    admin.setLastName("Health");
    admin.setPhone("5551234567");
    admin.setRole(Role.SYSTEM_ADMINISTRATOR);
    admin.setActive(true);
    userService.save(admin);
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }
}
