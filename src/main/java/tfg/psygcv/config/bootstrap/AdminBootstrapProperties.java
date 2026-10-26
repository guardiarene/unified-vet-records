package tfg.psygcv.config.bootstrap;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Initial administrator configuration, bound from the {@code app.bootstrap.admin.*} properties
 * (backed by the {@code ADMIN_EMAIL}, {@code ADMIN_PASSWORD}, {@code ADMIN_FIRST_NAME},
 * {@code ADMIN_LAST_NAME} and {@code ADMIN_PHONE} environment variables).
 *
 * <p>All five variables are required together: if any is defined (non-blank) while others are
 * missing, the application fails to start with a message naming the missing variables — never
 * their values. A blank value counts as not defined.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public class AdminBootstrapProperties {

  private String email;
  private String password;
  private String firstName;
  private String lastName;
  private String phone;

  /**
   * Returns whether the initial administrator is fully configured.
   *
   * @return {@code true} if all five variables have a non-blank value, {@code false} otherwise
   */
  public boolean isConfigured() {
    return missingEnvironmentVariables().isEmpty();
  }

  @PostConstruct
  void validateCompleteness() {
    List<String> missing = missingEnvironmentVariables();
    if (missing.isEmpty() || missing.size() == 5) {
      return;
    }
    throw new IllegalStateException(
        "Incomplete initial administrator configuration: missing environment variables "
            + String.join(", ", missing)
            + ". Define all five (ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_FIRST_NAME, ADMIN_LAST_NAME,"
            + " ADMIN_PHONE) or none of them.");
  }

  private List<String> missingEnvironmentVariables() {
    List<String> missing = new ArrayList<>();
    if (!StringUtils.hasText(email)) {
      missing.add("ADMIN_EMAIL");
    }
    if (!StringUtils.hasText(password)) {
      missing.add("ADMIN_PASSWORD");
    }
    if (!StringUtils.hasText(firstName)) {
      missing.add("ADMIN_FIRST_NAME");
    }
    if (!StringUtils.hasText(lastName)) {
      missing.add("ADMIN_LAST_NAME");
    }
    if (!StringUtils.hasText(phone)) {
      missing.add("ADMIN_PHONE");
    }
    return missing;
  }
}
