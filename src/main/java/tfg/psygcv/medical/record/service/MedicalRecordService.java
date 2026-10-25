package tfg.psygcv.medical.record.service;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import tfg.psygcv.medical.record.command.CreateMedicalRecordCommand;
import tfg.psygcv.medical.record.command.UpdateMedicalRecordCommand;
import tfg.psygcv.medical.record.entity.MedicalRecord;
import tfg.psygcv.medical.visit.entity.Anamnesis;

public interface MedicalRecordService {

  @PreAuthorize("@authz.canAccessMedicalRecord(authentication, #medicalRecordId)")
  MedicalRecord findCompleteById(Long medicalRecordId);

  List<MedicalRecord> findByVeterinarian(Long veterinarianId);

  @PreAuthorize("hasRole('VETERINARIAN')")
  MedicalRecord save(CreateMedicalRecordCommand command, Long veterinarianId);

  @PreAuthorize("@authz.canModifyMedicalRecord(authentication, #id)")
  MedicalRecord update(Long id, UpdateMedicalRecordCommand command, Long veterinarianId);

  Anamnesis computeCurrentAnamnesis(MedicalRecord medicalRecord);
}
