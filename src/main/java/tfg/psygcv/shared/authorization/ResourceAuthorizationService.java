package tfg.psygcv.shared.authorization;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import tfg.psygcv.appointment.repository.AppointmentRepository;
import tfg.psygcv.clinic.repository.MedicalServiceRepository;
import tfg.psygcv.clinic.repository.VeterinaryClinicRepository;
import tfg.psygcv.config.security.AuthenticatedUser;
import tfg.psygcv.medical.record.repository.MedicalRecordRepository;
import tfg.psygcv.medical.visit.repository.VisitRepository;
import tfg.psygcv.pet.repository.PetRepository;
import tfg.psygcv.user.entity.Role;

@Component("authz")
@RequiredArgsConstructor
public class ResourceAuthorizationService {

  private final AppointmentRepository appointmentRepository;
  private final VisitRepository visitRepository;
  private final PetRepository petRepository;
  private final MedicalRecordRepository medicalRecordRepository;
  private final MedicalServiceRepository medicalServiceRepository;
  private final VeterinaryClinicRepository veterinaryClinicRepository;

  public boolean canAccessAppointment(Authentication auth, Long appointmentId) {
    AuthenticatedUser user = principal(auth);
    if (isAdmin(user)) {
      return true;
    }
    if (user.getRole() == Role.CUSTOMER) {
      return appointmentRepository.existsByIdAndCustomer_Id(appointmentId, user.getId());
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null &&
        appointmentRepository.existsByIdAndClinic_Id(appointmentId, clinicId);
  }

  public boolean canAccessVisit(Authentication auth, Long visitId) {
    AuthenticatedUser user = principal(auth);
    if (isAdmin(user)) {
      return true;
    }
    if (user.getRole() == Role.CUSTOMER) {
      return visitRepository.existsByIdAndPetOwnerId(visitId, user.getId());
    }
    if (user.getRole() == Role.RECEPTIONIST) {
      return false;
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null && visitRepository.existsByIdAndClinicId(visitId, clinicId);
  }

  public boolean canModifyVisit(Authentication auth, Long visitId) {
    AuthenticatedUser user = principal(auth);
    if (user.getRole() != Role.VETERINARIAN) {
      return false;
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null
        && visitRepository.existsByIdAndVeterinarianId(visitId, user.getId())
        && visitRepository.existsByIdAndClinicId(visitId, clinicId);
  }

  public boolean canAccessPet(Authentication auth, Long petId) {
    AuthenticatedUser user = principal(auth);
    if (isAdmin(user)) {
      return true;
    }
    if (user.getRole() == Role.CUSTOMER) {
      return petRepository.existsByIdAndOwner_Id(petId, user.getId());
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null && petRepository.hasAppointmentAtClinic(petId, clinicId);
  }

  public boolean canAccessMedicalRecord(Authentication auth, Long recordId) {
    AuthenticatedUser user = principal(auth);
    if (isAdmin(user)) {
      return true;
    }
    if (user.getRole() == Role.CUSTOMER) {
      return medicalRecordRepository.existsByIdAndPetOwnerId(recordId, user.getId());
    }
    if (user.getRole() == Role.RECEPTIONIST) {
      return false;
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null && medicalRecordRepository.existsByIdAndClinicId(recordId, clinicId);
  }

  public boolean canModifyMedicalRecord(Authentication auth, Long recordId) {
    AuthenticatedUser user = principal(auth);
    if (user.getRole() != Role.VETERINARIAN) {
      return false;
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null && medicalRecordRepository.existsByIdAndClinicId(recordId, clinicId);
  }

  public boolean canManageMedicalService(Authentication auth, Long serviceId) {
    AuthenticatedUser user = principal(auth);
    if (isAdmin(user)) {
      return true;
    }
    if (user.getRole() != Role.VETERINARIAN) {
      return false;
    }
    Long clinicId = resolveClinicId(user);
    return clinicId != null && medicalServiceRepository.existsByIdAndClinic_Id(serviceId, clinicId);
  }

  private AuthenticatedUser principal(Authentication auth) {
    return (AuthenticatedUser) auth.getPrincipal();
  }

  private boolean isAdmin(AuthenticatedUser user) {
    return user.getRole() == Role.SYSTEM_ADMINISTRATOR;
  }

  private Long resolveClinicId(AuthenticatedUser user) {
    Long userId = user.getId();
    if (user.getRole() == Role.VETERINARIAN) {
      return veterinaryClinicRepository
          .findClinicIdByVeterinarianId(userId)
          .or(() -> veterinaryClinicRepository.findClinicIdByOwnerId(userId))
          .orElse(null);
    }
    if (user.getRole() == Role.RECEPTIONIST) {
      return veterinaryClinicRepository.findClinicIdByReceptionistId(userId).orElse(null);
    }
    return null;
  }
}
