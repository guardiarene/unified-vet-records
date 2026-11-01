package tfg.psygcv.medical.visit.service;

import java.time.LocalDate;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tfg.psygcv.appointment.repository.AppointmentQueryRepository;
import tfg.psygcv.clinic.entity.VeterinaryClinic;
import tfg.psygcv.exception.MessageKeys;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.medical.visit.command.CreateVisitCommand;
import tfg.psygcv.medical.visit.command.UpdateVisitCommand;
import tfg.psygcv.medical.visit.entity.VisitType;
import tfg.psygcv.pet.entity.Pet;
import tfg.psygcv.shared.validation.BaseValidator;
import tfg.psygcv.user.entity.User;

@Component
@RequiredArgsConstructor
public class VisitValidator extends BaseValidator {

  private final AppointmentQueryRepository appointmentQueryRepository;

  public void validateForCreation(CreateVisitCommand command, User veterinarian, Pet pet) {
    validateNotNull(command, "Visit command cannot be null");
    validateNotNull(veterinarian, "Veterinarian cannot be null");
    validateNotNull(pet, "Pet cannot be null");
    validateVisitFields(command.getDate(), command.getReasonForVisit(), command.getVisitType());
    validateVeterinarianClinicAccess(veterinarian, pet);
    validateAppointmentRequirement(command.getVisitType(), veterinarian, pet);
  }

  public void validateForUpdate(Long id, UpdateVisitCommand command, User veterinarian) {
    validateId(id);
    validateNotNull(command, "Visit command cannot be null");
    validateNotNull(veterinarian, "Veterinarian cannot be null");
    validateVisitFields(command.getDate(), command.getReasonForVisit(), command.getVisitType());
  }

  @Override
  public void validateId(Long id) {
    if (id == null || id <= 0) {
      throw new BusinessRuleException("Visit ID must be a positive number");
    }
  }

  private void validateVisitFields(LocalDate date, String reasonForVisit, VisitType visitType) {
    validateNotNull(date, "Visit date cannot be null");
    validateStringNotBlank(reasonForVisit, "Reason for visit");
    validateNotNull(visitType, "Visit type cannot be null");
  }

  private void validateVeterinarianClinicAccess(User veterinarian, Pet pet) {
    if (hasVeterinarianAccessToPet(veterinarian, pet)) {
      throw new BusinessRuleException(
          MessageKeys.PET_APPOINTMENTS_NONE_IN_CLINIC,
          "Pet has no registered appointments in any of the veterinarian's clinics");
    }
  }

  private void validateAppointmentRequirement(VisitType visitType, User veterinarian, Pet pet) {
    if (visitType == VisitType.EMERGENCY) {
      return;
    }
    if (hasVeterinarianAccessToPet(veterinarian, pet)) {
      throw new BusinessRuleException(
          MessageKeys.PET_APPOINTMENTS_NONE_IN_CLINIC,
          "Pet has no appointments in the veterinarian's clinics."
              + " Emergency visits can be created without a prior appointment.");
    }
  }

  private boolean hasVeterinarianAccessToPet(User veterinarian, Pet pet) {
    Set<VeterinaryClinic> veterinarianClinics = veterinarian.getClinicsOwned();
    VeterinaryClinic workClinic = veterinarian.getWorkClinic();
    if ((veterinarianClinics == null || veterinarianClinics.isEmpty()) && workClinic == null) {
      throw new BusinessRuleException(
          MessageKeys.USER_CLINIC_NONE,
          "The veterinarian has no registered clinics or assigned work clinic");
    }
    if (veterinarianClinics != null && !veterinarianClinics.isEmpty()) {
      if (appointmentQueryRepository.existsAppointmentByPetAndClinicsAndCustomer(
          pet, veterinarianClinics, pet.getOwner())) {
        return false;
      }
    }
    if (workClinic != null) {
      return !appointmentQueryRepository.existsAppointmentByPetAndClinicAndCustomer(
          pet, workClinic, pet.getOwner());
    }
    return true;
  }
}
