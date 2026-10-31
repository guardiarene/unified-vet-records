package tfg.psygcv.shared.logging;

import java.util.regex.Pattern;

/**
 * Utility for masking and sanitizing values before they are written to a log. Non-instantiable.
 */
public final class LogSanitizer {

  private static final int MAX_LENGTH = 100;
  private static final Pattern SAFE_DOMAIN = Pattern.compile("[A-Za-z0-9.-]+");

  private LogSanitizer() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Masks an email-like value, keeping only its first character and the domain. The domain is
   * kept only if it contains nothing but letters, digits, dots and hyphens; otherwise the whole
   * value is masked. The result is sanitized so it cannot contain control characters.
   */
  public static String maskEmail(String value) {
    if (value == null) {
      return null;
    }
    String sanitized = sanitize(value);
    int at = sanitized.indexOf('@');
    String masked;
    if (at >= 0 && SAFE_DOMAIN.matcher(sanitized.substring(at + 1)).matches()) {
      String local = sanitized.substring(0, at);
      String domain = sanitized.substring(at);
      String first = local.isEmpty() ? "*" : String.valueOf(local.charAt(0));
      masked = first + "***" + domain;
    } else {
      // No '@', or a domain no hostname can have: treat the whole value as sensitive and mask
      // it, so neither a password typed by mistake nor text injected after the '@' reaches the
      // log verbatim.
      if (sanitized.isEmpty()) {
        masked = sanitized;
      } else {
        masked = sanitized.charAt(0) + "***";
      }
    }
    if (masked.length() > MAX_LENGTH) {
      masked = masked.substring(0, MAX_LENGTH);
    }
    return masked;
  }

  /**
   * Removes control characters (including {@code \r} and {@code \n}) from the given text.
   */
  public static String sanitize(String text) {
    if (text == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (!Character.isISOControl(c)) {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}
