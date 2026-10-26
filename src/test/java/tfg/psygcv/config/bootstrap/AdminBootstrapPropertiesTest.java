package tfg.psygcv.config.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AdminBootstrapPropertiesTest {

  @Test
  void should_reportNotConfigured_when_noVariableIsDefined() {
    AdminBootstrapProperties properties = new AdminBootstrapProperties();

    assertThatCode(properties::validateCompleteness).doesNotThrowAnyException();
    assertThat(properties.isConfigured()).isFalse();
  }

  @Test
  void should_reportConfigured_when_allVariablesAreDefined() {
    AdminBootstrapProperties properties = fullyConfigured();

    assertThatCode(properties::validateCompleteness).doesNotThrowAnyException();
    assertThat(properties.isConfigured()).isTrue();
  }

  @Test
  void should_failNamingMissingVariables_when_configurationIsIncomplete() {
    AdminBootstrapProperties properties = new AdminBootstrapProperties();
    properties.setEmail("admin@clinic.test");

    assertThatThrownBy(properties::validateCompleteness)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ADMIN_PASSWORD")
        .hasMessageContaining("ADMIN_FIRST_NAME")
        .hasMessageContaining("ADMIN_LAST_NAME")
        .hasMessageContaining("ADMIN_PHONE")
        .hasMessageNotContaining("admin@clinic.test");
  }

  @Test
  void should_treatBlankValueAsUndefined_when_variableHasOnlyWhitespace() {
    AdminBootstrapProperties properties = fullyConfigured();
    properties.setPhone("   ");

    assertThat(properties.isConfigured()).isFalse();
    assertThatThrownBy(properties::validateCompleteness)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing environment variables ADMIN_PHONE.");
  }

  private static AdminBootstrapProperties fullyConfigured() {
    AdminBootstrapProperties properties = new AdminBootstrapProperties();
    properties.setEmail("admin@clinic.test");
    properties.setPassword("SuperSecret123");
    properties.setFirstName("Ada");
    properties.setLastName("Admin");
    properties.setPhone("5551234567");
    return properties;
  }
}
