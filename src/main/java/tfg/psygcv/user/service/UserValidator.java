package tfg.psygcv.user.service;

import org.springframework.stereotype.Component;
import tfg.psygcv.exception.MessageKeys;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.shared.validation.BaseValidator;
import tfg.psygcv.user.command.UpdateAdminUserCommand;
import tfg.psygcv.user.entity.User;

@Component
public class UserValidator extends BaseValidator {

  private static final int MIN_PASSWORD_LENGTH = 8;

  public void validateForCreation(User user) {
    validateNotNull(user, "User cannot be null");
    validateBasicFields(user);
    validatePassword(user.getPassword());
  }

  public void validateForUpdate(User user) {
    validateNotNull(user, "User cannot be null");
    validateId(user.getId());
    validateBasicFields(user);
  }

  public void validateForCompleteUpdate(UpdateAdminUserCommand command) {
    validateNotNull(command, "Update command cannot be null");
    validateStringNotBlank(command.getFirstName(), "First name");
    validateStringNotBlank(command.getLastName(), "Last name");
    validateEmail(command.getEmail());
    validateNotNull(command.getRole(), "User role cannot be null");
  }

  private void validateBasicFields(User user) {
    validateStringNotBlank(user.getFirstName(), "First name");
    validateStringNotBlank(user.getLastName(), "Last name");
    validateEmail(user.getEmail());
  }

  private void validatePassword(String password) {
    if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
      throw new BusinessRuleException(
          MessageKeys.USER_PASSWORD_TOO_SHORT,
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long",
          MIN_PASSWORD_LENGTH);
    }
  }
}
