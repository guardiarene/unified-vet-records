package tfg.psygcv.config.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tfg.psygcv.user.command.CreateInitialAdministratorCommand;
import tfg.psygcv.user.service.UserService;

/**
 * Thin startup runner for the initial administrator: reads the bootstrap configuration and
 * delegates to {@link UserService#createInitialAdministratorIfAbsent}.
 *
 * <p>Does nothing when no {@code ADMIN_*} variable is defined (the normal case for tests, CI and
 * development). The "only if no active administrator exists" rule lives in the service, not here.
 */
@RequiredArgsConstructor
@Component
public class InitialAdminBootstrapRunner implements ApplicationRunner {

  private final AdminBootstrapProperties properties;
  private final UserService userService;

  @Override
  public void run(ApplicationArguments args) {
    if (!properties.isConfigured()) {
      return;
    }
    userService.createInitialAdministratorIfAbsent(
        CreateInitialAdministratorCommand.builder()
            .email(properties.getEmail())
            .password(properties.getPassword())
            .firstName(properties.getFirstName())
            .lastName(properties.getLastName())
            .phone(properties.getPhone())
            .build());
  }
}
