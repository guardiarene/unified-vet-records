package tfg.psygcv.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.support.AuthTestFactory;
import tfg.psygcv.user.command.CreateInitialAdministratorCommand;
import tfg.psygcv.user.command.UpdateAdminUserCommand;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;
import tfg.psygcv.user.repository.UserRepository;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class UserServiceImplTest {

  private static final String ADMIN_EMAIL = "admin@clinic.test";
  private static final String ADMIN_PASSWORD = "SuperSecret123";

  @Mock
  private UserRepository userRepository;
  @Mock
  private PasswordEncoder passwordEncoder;

  private UserServiceImpl userService;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, passwordEncoder, new UserValidator());
  }

  @Test
  void should_doNothing_when_activeAdministratorAlreadyExists() {
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(1L);

    userService.createInitialAdministratorIfAbsent(initialAdminCommand());

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void should_createSystemAdministrator_when_noActiveAdministratorExists(CapturedOutput output) {
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(0L);
    when(userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL)).thenReturn(Optional.empty());
    when(passwordEncoder.encode(ADMIN_PASSWORD)).thenReturn("encoded-password");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User saved = invocation.getArgument(0);
              saved.setId(42L);
              return saved;
            });

    userService.createInitialAdministratorIfAbsent(initialAdminCommand());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getRole()).isEqualTo(Role.SYSTEM_ADMINISTRATOR);
    assertThat(saved.getActive()).isTrue();
    assertThat(saved.getPassword()).isEqualTo("encoded-password");
    assertThat(output).contains("userId=42").doesNotContain(ADMIN_PASSWORD);
  }

  @Test
  void should_throwBusinessRuleException_when_deactivatingLastAdministrator() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(1L);

    assertThatThrownBy(() -> userService.deactivate(1L))
        .isInstanceOf(BusinessRuleException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void should_deactivateAdministrator_when_anotherActiveAdministratorExists() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(2L);

    userService.deactivate(1L);

    assertThat(admin.getActive()).isFalse();
    verify(userRepository).save(admin);
  }

  @Test
  void should_deactivateNonAdministrator_when_countIsNotQueried() {
    User veterinarian = AuthTestFactory.user(2L, Role.VETERINARIAN);
    when(userRepository.findById(2L)).thenReturn(Optional.of(veterinarian));

    userService.deactivate(2L);

    assertThat(veterinarian.getActive()).isFalse();
    verify(userRepository).save(veterinarian);
    verify(userRepository, never()).countByRoleAndActiveTrue(any(Role.class));
  }

  @Test
  void should_throwBusinessRuleException_when_removingRoleFromLastAdministrator() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(1L);

    assertThatThrownBy(
        () -> userService.updateComplete(1L, updateCommand(Role.CUSTOMER, true)))
        .isInstanceOf(BusinessRuleException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void should_removeRole_when_anotherActiveAdministratorExists() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(2L);

    userService.updateComplete(1L, updateCommand(Role.CUSTOMER, true));

    assertThat(admin.getRole()).isEqualTo(Role.CUSTOMER);
    verify(userRepository).save(admin);
  }

  @Test
  void should_throwBusinessRuleException_when_updateDeactivatesLastAdministrator() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
    when(userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR)).thenReturn(1L);

    assertThatThrownBy(
        () -> userService.updateComplete(
            1L, updateCommand(Role.SYSTEM_ADMINISTRATOR, false)))
        .isInstanceOf(BusinessRuleException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void should_keepActiveState_when_activeIsNull() {
    User admin = AuthTestFactory.user(1L, Role.SYSTEM_ADMINISTRATOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

    userService.updateComplete(1L, updateCommand(Role.SYSTEM_ADMINISTRATOR, null));

    assertThat(admin.getActive()).isTrue();
    verify(userRepository).save(admin);
    verify(userRepository, never()).countByRoleAndActiveTrue(any(Role.class));
  }

  @Test
  void should_throwUsernameNotFoundException_when_loginEmailHasInvalidFormat() {
    assertThatThrownBy(() -> userService.loadUserByUsername("no-es-un-email"))
        .isInstanceOf(UsernameNotFoundException.class)
        .hasMessage("User not found.");

    verifyNoInteractions(userRepository);
  }

  @Test
  void should_throwBusinessRuleException_when_roleIsNull() {
    assertThatThrownBy(() -> userService.updateComplete(1L, updateCommand(null, true)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("User role cannot be null");

    verifyNoInteractions(userRepository);
  }

  private static CreateInitialAdministratorCommand initialAdminCommand() {
    return CreateInitialAdministratorCommand.builder()
        .firstName("Ada")
        .lastName("Admin")
        .email(ADMIN_EMAIL)
        .password(ADMIN_PASSWORD)
        .phone("5551234567")
        .build();
  }

  private static UpdateAdminUserCommand updateCommand(Role role, Boolean active) {
    return UpdateAdminUserCommand.builder()
        .firstName("Ada")
        .lastName("Admin")
        .email("updated@clinic.test")
        .phone("5551234567")
        .role(role)
        .active(active)
        .build();
  }
}
