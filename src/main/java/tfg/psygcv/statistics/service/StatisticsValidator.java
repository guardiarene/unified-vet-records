package tfg.psygcv.statistics.service;

import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tfg.psygcv.exception.MessageKeys;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.shared.validation.BaseValidator;

@Component
@RequiredArgsConstructor
public class StatisticsValidator extends BaseValidator {

  private static final int MAX_RANGE_YEARS = 2;

  private final Clock clock;

  public void validateUserId(Long userId) {
    validateId(userId);
  }

  public void validateDateRange(LocalDate startDate, LocalDate endDate) {
    if (startDate == null) {
      throw new BusinessRuleException(
          MessageKeys.REPORT_DATE_RANGE_MISSING, "Start date cannot be null");
    }
    if (endDate == null) {
      throw new BusinessRuleException(
          MessageKeys.REPORT_DATE_RANGE_MISSING, "End date cannot be null");
    }
    if (startDate.isAfter(endDate)) {
      throw new BusinessRuleException(
          MessageKeys.REPORT_DATE_RANGE_START_AFTER_END, "Start date cannot be after end date");
    }
    if (endDate.isAfter(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          MessageKeys.REPORT_DATE_RANGE_END_IN_FUTURE, "End date cannot be in the future");
    }
    if (startDate.plusYears(MAX_RANGE_YEARS).isBefore(endDate)) {
      throw new BusinessRuleException(
          MessageKeys.REPORT_DATE_RANGE_TOO_LONG,
          "Date range cannot exceed " + MAX_RANGE_YEARS + " years",
          MAX_RANGE_YEARS);
    }
  }
}
