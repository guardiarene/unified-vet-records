package tfg.psygcv.config.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tfg.psygcv.user.command.CreateInitialAdministratorCommand;
import tfg.psygcv.user.service.UserService;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InitialAdminBootstrapRunnerTest {

  private static final String ADMIN_PASSWORD = "SuperSecret123";

  @Mock
  private UserService userService;

  @Test
  void should_doNothing_when_noVariableIsDefined() {
    InitialAdminBootstrapRunner runner =
        new InitialAdminBootstrapRunner(new AdminBootstrapProperties(), userService);

    runner.run(mock(ApplicationArguments.class));

    verifyNoInteractions(userService);
  }

  @Test
  void should_createInitialAdministrator_when_allVariablesAreDefined(CapturedOutput output) {
    InitialAdminBootstrapRunner runner =
        new InitialAdminBootstrapRunner(fullyConfigured(), userService);

    runner.run(mock(ApplicationArguments.class));

    ArgumentCaptor<CreateInitialAdministratorCommand> captor =
        ArgumentCaptor.forClass(CreateInitialAdministratorCommand.class);
    verify(userService).createInitialAdministratorIfAbsent(captor.capture());
    CreateInitialAdministratorCommand command = captor.getValue();
    assertThat(command.getEmail()).isEqualTo("admin@clinic.test");
    assertThat(command.getPassword()).isEqualTo(ADMIN_PASSWORD);
    assertThat(command.getFirstName()).isEqualTo("Ada");
    assertThat(command.getLastName()).isEqualTo("Admin");
    assertThat(command.getPhone()).isEqualTo("5551234567");
    assertThat(output).doesNotContain(ADMIN_PASSWORD);
  }

  private static AdminBootstrapProperties fullyConfigured() {
    AdminBootstrapProperties properties = new AdminBootstrapProperties();
    properties.setEmail("admin@clinic.test");
    properties.setPassword(ADMIN_PASSWORD);
    properties.setFirstName("Ada");
    properties.setLastName("Admin");
    properties.setPhone("5551234567");
    return properties;
  }
}
