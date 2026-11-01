package tfg.psygcv.exception;

/**
 * Keys of the user-facing messages in {@code messages.properties}.
 *
 * <p>Every key thrown with a {@link tfg.psygcv.exception.domain.LocalizedException} is declared
 * here; {@code MessageKeysTest} verifies that each constant exists in the properties file.
 */
public final class MessageKeys {

  public static final String BUSINESS_GENERIC = "error.business.generic";
  public static final String NOT_FOUND_GENERIC = "error.notfound.generic";

  public static final String APPOINTMENT_DATE_PAST = "appointment.date.past";

  public static final String REPORT_DATE_RANGE_START_AFTER_END =
      "report.date-range.start-after-end";
  public static final String REPORT_DATE_RANGE_END_IN_FUTURE = "report.date-range.end-in-future";
  public static final String REPORT_DATE_RANGE_TOO_LONG = "report.date-range.too-long";
  public static final String REPORT_DATE_RANGE_MISSING = "report.date-range.missing";

  public static final String USER_EMAIL_DUPLICATE = "user.email.duplicate";
  public static final String USER_PASSWORD_TOO_SHORT = "user.password.too-short";
  public static final String USER_CLINIC_NONE = "user.clinic.none";
  public static final String USER_ADMIN_LAST_ACTIVE = "user.admin.last-active";

  public static final String PET_APPOINTMENTS_NONE_IN_CLINIC = "pet.appointments.none-in-clinic";

  public static final String MEDICAL_RECORD_ALREADY_EXISTS = "medical-record.already-exists";

  private MessageKeys() {
    throw new UnsupportedOperationException("Utility class");
  }
}
