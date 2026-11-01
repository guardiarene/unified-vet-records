package tfg.psygcv.exception.domain;

public class BusinessRuleException extends LocalizedException {

  public BusinessRuleException(String message) {
    super(message);
  }

  public BusinessRuleException(String messageKey, String message, Object... args) {
    super(messageKey, message, args);
  }
}
