package tfg.psygcv.exception.domain;

public class DuplicateEmailException extends BusinessRuleException {

  public DuplicateEmailException(String message) {
    super(message);
  }

  public DuplicateEmailException(String messageKey, String message) {
    super(messageKey, message);
  }
}
