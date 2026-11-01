package tfg.psygcv.exception.domain;

import lombok.Getter;

/**
 * Base type for domain exceptions whose user-facing text is resolved from
 * {@code messages.properties}.
 *
 * <p>{@link #getMessage()} keeps the English message for the logs. The user sees the text of
 * {@link #getMessageKey()}, formatted with {@link #getArgs()}; when the key is {@code null}, the
 * handler shows the generic message of the exception type.
 */
public abstract class LocalizedException extends RuntimeException {

  private static final Object[] NO_ARGS = new Object[0];

  /**
   * -- GETTER --
   * Returns the message key for the user-facing text, or
   * if there is none.
   */
  @Getter
  private final String messageKey;
  private final Object[] args;

  protected LocalizedException(String message) {
    this(null, message);
  }

  protected LocalizedException(String messageKey, String message, Object... args) {
    super(message);
    this.messageKey = messageKey;
    this.args = args != null ? args.clone() : NO_ARGS;
  }

  /**
   * Returns a copy of the arguments used to format the user-facing text.
   */
  public Object[] getArgs() {
    return args.clone();
  }
}
