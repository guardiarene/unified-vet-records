package tfg.psygcv.shared.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static tfg.psygcv.support.AuthTestFactory.authentication;
import static tfg.psygcv.support.AuthTestFactory.user;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import tfg.psygcv.appointment.repository.AppointmentRepository;
import tfg.psygcv.clinic.repository.MedicalServiceRepository;
import tfg.psygcv.clinic.repository.VeterinaryClinicRepository;
import tfg.psygcv.medical.record.repository.MedicalRecordRepository;
import tfg.psygcv.medical.visit.repository.VisitRepository;
import tfg.psygcv.pet.repository.PetRepository;
import tfg.psygcv.user.entity.Role;

@ExtendWith(MockitoExtension.class)
class ResourceAuthorizationServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long RESOURCE_ID = 10L;
  private static final Long WORK_CLINIC_ID = 20L;
  private static final Long OWN_CLINIC_ID = 21L;

  @Mock
  private AppointmentRepository appointmentRepository;
  @Mock
  private VisitRepository visitRepository;
  @Mock
  private PetRepository petRepository;
  @Mock
  private MedicalRecordRepository medicalRecordRepository;
  @Mock
  private MedicalServiceRepository medicalServiceRepository;
  @Mock
  private VeterinaryClinicRepository veterinaryClinicRepository;

  @InjectMocks
  private ResourceAuthorizationService authorizationService;

  private Authentication authenticationFor(Role role) {
    return authentication(user(USER_ID, role));
  }

  private void verifyNoRepositoryInteractions() {
    verifyNoInteractions(
        appointmentRepository,
        visitRepository,
        petRepository,
        medicalRecordRepository,
        medicalServiceRepository,
        veterinaryClinicRepository);
  }

  @Nested
  class CanAccessAppointment {

    @Test
    void should_returnTrue_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isTrue();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_customerOwnsAppointment() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(appointmentRepository.existsByIdAndCustomer_Id(RESOURCE_ID, USER_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_customerDoesNotOwnAppointment() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(appointmentRepository.existsByIdAndCustomer_Id(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(appointmentRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(appointmentRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnTrue_when_receptionistClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(appointmentRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_receptionistClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(appointmentRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_receptionistHasNoClinic() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }
  }

  @Nested
  class CanAccessPet {

    @Test
    void should_returnTrue_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_customerOwnsPet() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(petRepository.existsByIdAndOwner_Id(RESOURCE_ID, USER_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_customerDoesNotOwnPet() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(petRepository.existsByIdAndOwner_Id(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnTrue_when_receptionistClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_receptionistClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_receptionistHasNoClinic() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }
  }

  @Nested
  class CanAccessVisit {

    @Test
    void should_returnTrue_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isTrue();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_customerOwnsVisit() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(visitRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_customerDoesNotOwnVisit() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(visitRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_receptionistHasAppointmentAtHerClinic() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      // Lenient: these stubs are consumed only if the RECEPTIONIST exclusion is removed (regression).
      lenient()
          .when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      lenient()
          .when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isFalse();
    }
  }

  @Nested
  class CanAccessMedicalRecord {

    @Test
    void should_returnTrue_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isTrue();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_customerOwnsRecord() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(medicalRecordRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_customerDoesNotOwnRecord() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(medicalRecordRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_receptionistHasAppointmentAtHerClinic() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);
      // Lenient: these stubs are consumed only if the RECEPTIONIST exclusion is removed (regression).
      lenient()
          .when(veterinaryClinicRepository.findClinicIdByReceptionistId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      lenient()
          .when(medicalRecordRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalRecordRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalRecordRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isFalse();
    }
  }

  @Nested
  class CanModifyVisit {

    @Test
    void should_returnFalse_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_customerOwnsVisit() {
      Authentication auth = authenticationFor(Role.CUSTOMER);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_userIsReceptionist() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_veterinarianIsAuthorAndClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndVeterinarianId(RESOURCE_ID, USER_ID)).thenReturn(true);
      when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicNoLongerHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndVeterinarianId(RESOURCE_ID, USER_ID)).thenReturn(true);
      when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(false);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_colleagueAtSameClinicIsNotAuthor() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndVeterinarianId(RESOURCE_ID, USER_ID)).thenReturn(false);
      // Lenient: only consumed if the author condition is removed (regression).
      lenient()
          .when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianFromAnotherClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndVeterinarianId(RESOURCE_ID, USER_ID)).thenReturn(false);
      // Lenient: only consumed if the author condition is removed (regression).
      lenient()
          .when(visitRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
      verifyNoInteractions(visitRepository);
    }
  }

  @Nested
  class CanModifyMedicalRecord {

    @Test
    void should_returnFalse_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_customerOwnsRecord() {
      Authentication auth = authenticationFor(Role.CUSTOMER);

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_userIsReceptionist() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicHasAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalRecordRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotHaveAppointment() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalRecordRepository.existsByIdAndClinicId(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canModifyMedicalRecord(auth, RESOURCE_ID)).isFalse();
      verifyNoInteractions(medicalRecordRepository);
    }
  }

  @Nested
  class CanManageMedicalService {

    @Test
    void should_returnTrue_when_userIsAdministrator() {
      Authentication auth = authenticationFor(Role.SYSTEM_ADMINISTRATOR);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isTrue();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_userIsCustomer() {
      Authentication auth = authenticationFor(Role.CUSTOMER);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnFalse_when_userIsReceptionist() {
      Authentication auth = authenticationFor(Role.RECEPTIONIST);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isFalse();
      verifyNoRepositoryInteractions();
    }

    @Test
    void should_returnTrue_when_veterinarianClinicOwnsService() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalServiceRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(true);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isTrue();
    }

    @Test
    void should_returnFalse_when_veterinarianClinicDoesNotOwnService() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalServiceRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_veterinarianHasNoClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID)).thenReturn(Optional.empty());

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isFalse();
      verifyNoInteractions(medicalServiceRepository);
    }
  }

  @Nested
  class ClinicResolution {

    @Test
    void should_useWorkClinicWithoutConsultingOwnClinic_when_veterinarianHasWorkClinic() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, WORK_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
      verify(veterinaryClinicRepository, never()).findClinicIdByOwnerId(USER_ID);
    }

    @Test
    void should_useOwnClinic_when_veterinarianHasNoWorkClinicButOwnsOne() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.empty());
      when(veterinaryClinicRepository.findClinicIdByOwnerId(USER_ID))
          .thenReturn(Optional.of(OWN_CLINIC_ID));
      when(petRepository.hasAppointmentAtClinic(RESOURCE_ID, OWN_CLINIC_ID)).thenReturn(true);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isTrue();
    }
  }

  @Nested
  class NonExistentResourceId {

    @Test
    void should_returnFalse_when_appointmentDoesNotExist() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(appointmentRepository.existsByIdAndCustomer_Id(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessAppointment(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_petDoesNotExist() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(petRepository.existsByIdAndOwner_Id(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessPet(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_visitDoesNotExist() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(visitRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessVisit(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_medicalRecordDoesNotExist() {
      Authentication auth = authenticationFor(Role.CUSTOMER);
      when(medicalRecordRepository.existsByIdAndPetOwnerId(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canAccessMedicalRecord(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_medicalServiceDoesNotExist() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(medicalServiceRepository.existsByIdAndClinic_Id(RESOURCE_ID, WORK_CLINIC_ID))
          .thenReturn(false);

      assertThat(authorizationService.canManageMedicalService(auth, RESOURCE_ID)).isFalse();
    }

    @Test
    void should_returnFalse_when_visitDoesNotExistForAuthor() {
      Authentication auth = authenticationFor(Role.VETERINARIAN);
      when(veterinaryClinicRepository.findClinicIdByVeterinarianId(USER_ID))
          .thenReturn(Optional.of(WORK_CLINIC_ID));
      when(visitRepository.existsByIdAndVeterinarianId(RESOURCE_ID, USER_ID)).thenReturn(false);

      assertThat(authorizationService.canModifyVisit(auth, RESOURCE_ID)).isFalse();
    }
  }
}
