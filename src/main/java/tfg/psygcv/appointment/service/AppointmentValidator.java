package tfg.psygcv.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tfg.psygcv.appointment.command.RescheduleAppointmentCommand;
import tfg.psygcv.appointment.command.ScheduleAppointmentCommand;
import tfg.psygcv.appointment.entity.Appointment;
import tfg.psygcv.appointment.entity.AppointmentStatus;
import tfg.psygcv.clinic.entity.VeterinaryClinic;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.pet.entity.Pet;
import tfg.psygcv.shared.validation.BaseValidator;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentValidator extends BaseValidator {

  private final Clock clock;

  public void validateClientAppointmentCreation(
      LocalDate date, Long petId, Long serviceId, Long clinicId, Long clientId) {
    validateNotNull(date, "Appointment date cannot be null");
    if (date.isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException("Appointment date cannot be in the past");
    }
    validateId(petId);
    validateId(serviceId);
    validateId(clinicId);
    validateId(clientId);
  }

  public void validateReceptionistAppointmentCreation(
      ScheduleAppointmentCommand command, Long receptionistId) {
    validateNotNull(command, "Schedule command cannot be null");
    validateNotNull(command.getDate(), "Appointment date is required");
    if (command.getDate().isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException("Appointment date cannot be in the past");
    }
    validateNotNull(command.getTime(), "Appointment time is required");
    validateId(command.getPetId());
    validateId(command.getCustomerId());
    validateId(command.getServiceId());
    validateId(receptionistId);
  }

  public void validateCustomerRole(User user) {
    validateNotNull(user, "User cannot be null");
    if (user.getRole() != Role.CUSTOMER) {
      log.warn("Appointment assigned to a non-customer user [userId={}, role={}]",
          user.getId(), user.getRole());
      throw new BusinessRuleException("Appointment can only be assigned to a customer");
    }
  }

  public void validatePetOwnership(Pet pet, User customer) {
    validateNotNull(pet, "Pet cannot be null");
    validateNotNull(customer, "Customer cannot be null");
    if (!pet.getOwner().getId().equals(customer.getId())) {
      log.warn("Pet does not belong to customer [petId={}, customerId={}]",
          pet.getId(), customer.getId());
      throw new BusinessRuleException("Pet does not belong to the specified customer");
    }
  }

  public void validateReschedule(Long appointmentId, RescheduleAppointmentCommand command) {
    validateId(appointmentId);
    validateNotNull(command, "Reschedule command cannot be null");
    validateNotNull(command.getDate(), "New appointment date is required");
    validateNotNull(command.getTime(), "New appointment time is required");
    if (command.getDate().isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException("New appointment date cannot be in the past");
    }
    validateNotNull(command.getMedicalServiceId(), "Medical service is required for rescheduling");
  }

  public void validateAppointment(Appointment appointment) {
    validateNotNull(appointment, "Appointment cannot be null");
  }

  public void validateStatus(AppointmentStatus status) {
    validateNotNull(status, "Appointment status cannot be null");
  }

  public void validateClinics(List<VeterinaryClinic> clinics) {
    if (clinics == null || clinics.isEmpty()) {
      throw new BusinessRuleException("Clinics list cannot be null or empty");
    }
  }
}
