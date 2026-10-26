package tfg.psygcv.user.command;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class CreateInitialAdministratorCommand {

  private final String firstName;
  private final String lastName;
  private final String email;
  private final String password;
  private final String phone;
}
