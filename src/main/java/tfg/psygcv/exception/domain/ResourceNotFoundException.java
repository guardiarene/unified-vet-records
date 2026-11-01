package tfg.psygcv.exception.domain;

public class ResourceNotFoundException extends LocalizedException {

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Long resourceId) {
    super(message + " (ID: " + resourceId + ")");
  }

  public ResourceNotFoundException(String messageKey, String message, Object... args) {
    super(messageKey, message, args);
  }
}
