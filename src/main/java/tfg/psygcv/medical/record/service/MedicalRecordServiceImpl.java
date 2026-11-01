package tfg.psygcv.medical.record.service;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tfg.psygcv.appointment.repository.AppointmentQueryRepository;
import tfg.psygcv.clinic.entity.VeterinaryClinic;
import tfg.psygcv.clinic.repository.VeterinaryClinicRepository;
import tfg.psygcv.exception.MessageKeys;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.medical.record.command.CreateMedicalRecordCommand;
import tfg.psygcv.medical.record.command.UpdateMedicalRecordCommand;
import tfg.psygcv.medical.record.entity.MedicalRecord;
import tfg.psygcv.medical.record.repository.MedicalRecordQueryRepository;
import tfg.psygcv.medical.record.repository.MedicalRecordRepository;
import tfg.psygcv.medical.visit.entity.Anamnesis;
import tfg.psygcv.medical.visit.entity.Visit;
import tfg.psygcv.medical.visit.service.VisitService;
import tfg.psygcv.pet.entity.Pet;
import tfg.psygcv.pet.repository.PetRepository;
import tfg.psygcv.user.entity.User;
import tfg.psygcv.user.service.UserService;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

  private final MedicalRecordRepository medicalRecordRepository;
  private final MedicalRecordQueryRepository medicalRecordQueryRepository;
  private final VeterinaryClinicRepository veterinaryClinicRepository;
  private final PetRepository petRepository;
  private final AppointmentQueryRepository appointmentQueryRepository;
  private final VisitService visitService;
  private final UserService userService;
  private final MedicalRecordValidator medicalRecordValidator;

  @Override
  @Transactional(readOnly = true)
  public MedicalRecord findCompleteById(Long medicalRecordId) {
    medicalRecordValidator.validateId(medicalRecordId);
    MedicalRecord medicalRecord =
        medicalRecordQueryRepository.findCompleteForViewing(medicalRecordId).orElseThrow(
            () -> new ResourceNotFoundException(
                "Medical record not found with ID: " + medicalRecordId));
    List<Visit> visits = visitService.findByMedicalRecord(medicalRecordId);
    medicalRecord.getVisits().clear();
    medicalRecord.getVisits().addAll(visits);
    medicalRecordQueryRepository.findWithVaccines(medicalRecordId);
    return medicalRecord;
  }

  @Override
  @Transactional(readOnly = true)
  public List<MedicalRecord> findByVeterinarian(Long veterinarianId) {
    medicalRecordValidator.validateId(veterinarianId);
    VeterinaryClinic clinic = veterinaryClinicRepository.findByVeterinarianId(veterinarianId)
        .or(() -> veterinaryClinicRepository.findByOwnerIdOptional(veterinarianId)).orElseThrow(
            () -> new ResourceNotFoundException(
                "Veterinary clinic not found for veterinarian ID: " + veterinarianId));
    return medicalRecordRepository.findByClinicId(clinic.getId());
  }

  @Override
  @Transactional
  public MedicalRecord save(CreateMedicalRecordCommand command, Long veterinarianId) {
    User veterinarianWithClinicContext = userService.findByIdWithClinicContext(veterinarianId);
    medicalRecordValidator.validateForCreation(command, veterinarianId);
    Pet pet = petRepository.findByIdAndActive(command.getPetId())
        .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
    validatePetAppointments(pet, veterinarianWithClinicContext);
    validateExistingMedicalRecord(pet);
    MedicalRecord newMedicalRecord = createMedicalRecord(pet);
    if (command.getGeneralObservations() != null) {
      newMedicalRecord.setGeneralObservations(command.getGeneralObservations());
    }
    MedicalRecord saved = medicalRecordRepository.save(newMedicalRecord);
    log.info("Medical record created [recordId={}, petId={}]",
        saved.getId(), command.getPetId());
    return saved;
  }

  @Override
  @Transactional
  public MedicalRecord update(Long id, UpdateMedicalRecordCommand command, Long veterinarianId) {
    medicalRecordValidator.validateForUpdate(id, command, veterinarianId);
    MedicalRecord existingRecord = medicalRecordRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
    existingRecord.setGeneralObservations(command.getGeneralObservations());
    return medicalRecordRepository.save(existingRecord);
  }

  private MedicalRecord createMedicalRecord(Pet pet) {
    MedicalRecord medicalRecord = new MedicalRecord();
    medicalRecord.setPet(pet);
    medicalRecord.setActive(true);
    pet.setMedicalRecord(medicalRecord);
    return medicalRecord;
  }

  private void validatePetAppointments(Pet pet, User veterinarian) {
    Set<VeterinaryClinic> veterinarianClinics = veterinarian.getClinicsOwned();
    VeterinaryClinic workClinic = veterinarian.getWorkClinic();
    boolean hasAppointments = false;
    if (veterinarianClinics != null && !veterinarianClinics.isEmpty()) {
      hasAppointments = appointmentQueryRepository.existsAppointmentByPetAndClinicsAndCustomer(pet,
          veterinarianClinics, pet.getOwner());
    }
    if (!hasAppointments && workClinic != null) {
      hasAppointments =
          appointmentQueryRepository.existsAppointmentByPetAndClinicAndCustomer(pet, workClinic,
              pet.getOwner());
    }
    if (!hasAppointments) {
      throw new BusinessRuleException(
          MessageKeys.PET_APPOINTMENTS_NONE_IN_CLINIC,
          "Pet has no registered appointments in veterinarian's clinics");
    }
  }

  private void validateExistingMedicalRecord(Pet pet) {
    if (pet.getMedicalRecord() != null) {
      throw new BusinessRuleException(
          MessageKeys.MEDICAL_RECORD_ALREADY_EXISTS,
          "This pet already has a registered medical record");
    }
  }

  @Override
  public Anamnesis computeCurrentAnamnesis(MedicalRecord medicalRecord) {
    Anamnesis current = new Anamnesis();
    for (Visit visit : medicalRecord.getVisits()) {
      Anamnesis source = visit.getAnamnesis();
      if (source != null) {
        mergeAnamnesis(current, source);
      }
    }
    return current;
  }

  private void mergeAnamnesis(Anamnesis current, Anamnesis source) {
    mergeTextFields(current, source);
    mergeReproductiveField(current, source);
    mergeDateFields(current, source);
  }

  private void mergeTextFields(Anamnesis current, Anamnesis source) {
    if (current.getAllergies() == null && source.getAllergies() != null) {
      current.setAllergies(source.getAllergies());
    }
    if (current.getPreviousDiseases() == null && source.getPreviousDiseases() != null) {
      current.setPreviousDiseases(source.getPreviousDiseases());
    }
    if (current.getSurgeries() == null && source.getSurgeries() != null) {
      current.setSurgeries(source.getSurgeries());
    }
    if (current.getCurrentMedications() == null && source.getCurrentMedications() != null) {
      current.setCurrentMedications(source.getCurrentMedications());
    }
    if (current.getDiet() == null && source.getDiet() != null) {
      current.setDiet(source.getDiet());
    }
  }

  private void mergeReproductiveField(Anamnesis current, Anamnesis source) {
    if (current.getReproductiveStatus() == null && source.getReproductiveStatus() != null) {
      current.setReproductiveStatus(source.getReproductiveStatus());
    }
  }

  private void mergeDateFields(Anamnesis current, Anamnesis source) {
    if (current.getLastDewormingDate() == null && source.getLastDewormingDate() != null) {
      current.setLastDewormingDate(source.getLastDewormingDate());
    }
    if (current.getLastHeatDate() == null && source.getLastHeatDate() != null) {
      current.setLastHeatDate(source.getLastHeatDate());
    }
    if (current.getLastBirthDate() == null && source.getLastBirthDate() != null) {
      current.setLastBirthDate(source.getLastBirthDate());
    }
  }
}
