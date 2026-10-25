package tfg.psygcv.statistics.service;

import java.time.LocalDate;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

public interface StatisticsService {

  @PreAuthorize("hasRole('VETERINARIAN')")
  Map<String, Long> getCommonDiseases(Long userId, LocalDate startDate, LocalDate endDate);

  @PreAuthorize("hasRole('VETERINARIAN')")
  Map<String, Long> getFrequentTreatments(Long userId, LocalDate startDate, LocalDate endDate);

  Map<LocalDate, Long> getAppointmentsByDate(Long userId, LocalDate startDate, LocalDate endDate);

  Map<String, Long> getRequestedServices(Long userId, LocalDate startDate, LocalDate endDate);
}
