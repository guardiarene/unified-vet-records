package tfg.psygcv.medical.visit.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import tfg.psygcv.medical.visit.command.CreateVisitCommand;
import tfg.psygcv.medical.visit.command.UpdateVisitCommand;
import tfg.psygcv.medical.visit.entity.Visit;

public interface VisitService {

  @PreAuthorize("hasRole('VETERINARIAN')")
  Visit createVisit(Long medicalRecordId, CreateVisitCommand command, Long veterinarianId);

  @PreAuthorize("@authz.canModifyVisit(authentication, #visitId)")
  Visit updateVisit(Long visitId, UpdateVisitCommand command, Long veterinarianId);

  @PreAuthorize("@authz.canAccessVisit(authentication, #visitId)")
  Visit findCompleteById(Long visitId);

  List<Visit> findByMedicalRecord(Long medicalRecordId);

  Page<Visit> findByMedicalRecord(Long medicalRecordId, Pageable pageable);

  List<Visit> findByMedicalRecordAndDateRange(
      Long medicalRecordId, LocalDate startDate, LocalDate endDate);

  Visit findLatestVisit(Long medicalRecordId);

  @PreAuthorize("@authz.canModifyVisit(authentication, #visitId)")
  Long deleteVisit(Long visitId);
}
