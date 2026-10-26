package tfg.psygcv.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tfg.psygcv.clinic.entity.VeterinaryClinic;
import tfg.psygcv.config.security.AuthenticatedUser;
import tfg.psygcv.exception.domain.BusinessRuleException;
import tfg.psygcv.exception.domain.DuplicateEmailException;
import tfg.psygcv.exception.domain.ResourceNotFoundException;
import tfg.psygcv.user.command.CreateAdminUserCommand;
import tfg.psygcv.user.command.CreateInitialAdministratorCommand;
import tfg.psygcv.user.command.CreateStaffCommand;
import tfg.psygcv.user.command.RegisterCustomerCommand;
import tfg.psygcv.user.command.RegisterVeterinarianCommand;
import tfg.psygcv.user.command.UpdateAdminUserCommand;
import tfg.psygcv.user.command.UpdateUserProfileCommand;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;
import tfg.psygcv.user.repository.UserRepository;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserServiceImpl implements UserDetailsService, UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserValidator userValidator;

  @Override
  @Transactional(readOnly = true)
  public @NonNull UserDetails loadUserByUsername(@NonNull String email)
      throws UsernameNotFoundException {
    User user = findByEmail(email);
    if (user == null) {
      throw new UsernameNotFoundException("User not found with email: " + email);
    }
    return AuthenticatedUser.from(user);
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  public List<User> findActiveCustomers() {
    return userRepository.findByRoleAndActive(Role.CUSTOMER, true);
  }

  @Override
  public User findById(Long userId) {
    userValidator.validateId(userId);
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", userId));
  }

  @Override
  public User findByIdWithClinicContext(Long userId) {
    userValidator.validateId(userId);
    return userRepository
        .findByIdWithClinicContext(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", userId));
  }

  @Override
  public User findByEmail(String email) {
    userValidator.validateEmail(email);
    return userRepository
        .findByEmailAndActiveTrue(email)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
  }

  @Override
  @Transactional
  public User save(User user) {
    userValidator.validateForCreation(user);
    if (userRepository.findByEmailAndActiveTrue(user.getEmail()).isPresent()) {
      log.warn("Duplicate email registration attempt [email={}]", user.getEmail());
      throw new DuplicateEmailException(
          "A user with this email already exists: " + user.getEmail());
    }
    encodePassword(user);
    return userRepository.save(user);
  }

  @Override
  @Transactional
  public User registerCustomer(RegisterCustomerCommand command) {
    User user = new User();
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPassword(command.getPassword());
    user.setPhone(command.getPhone());
    user.setRole(Role.CUSTOMER);
    user.setActive(true);
    return save(user);
  }

  @Override
  @Transactional
  public void saveComplete(CreateAdminUserCommand command) {
    User user = new User();
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPassword(command.getPassword());
    user.setPhone(command.getPhone());
    user.setRole(command.getRole());
    user.setActive(true);
    save(user);
  }

  @Override
  @Transactional
  public void createInitialAdministratorIfAbsent(CreateInitialAdministratorCommand command) {
    if (userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR) > 0) {
      log.info("Active administrator already present; skipping initial administrator creation");
      return;
    }
    User user = new User();
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPassword(command.getPassword());
    user.setPhone(command.getPhone());
    user.setRole(Role.SYSTEM_ADMINISTRATOR);
    user.setActive(true);
    User saved = save(user);
    log.info("Initial administrator created [userId={}]", saved.getId());
  }

  @Override
  @Transactional
  public void updateComplete(Long userId, UpdateAdminUserCommand command) {
    userValidator.validateId(userId);
    userValidator.validateForCompleteUpdate(command);
    User existingUser = findById(userId);
    boolean deactivating = Boolean.FALSE.equals(command.getActive());
    boolean losingRole = command.getRole() != Role.SYSTEM_ADMINISTRATOR;
    if (removesLastActiveAdministrator(existingUser, deactivating, losingRole)) {
      throw new BusinessRuleException(
          "The last active system administrator cannot be deactivated or lose its role");
    }
    existingUser.setFirstName(command.getFirstName());
    existingUser.setLastName(command.getLastName());
    existingUser.setPhone(command.getPhone());
    existingUser.setEmail(command.getEmail());
    existingUser.setRole(command.getRole());
    if (command.getActive() != null) {
      existingUser.setActive(command.getActive());
    }
    if (command.getPassword() != null && !command.getPassword().isEmpty()) {
      encodePassword(existingUser, command.getPassword());
    }
    userRepository.save(existingUser);
  }

  @Override
  @Transactional
  public void updateVeterinarianProfile(Long veterinarianId, UpdateUserProfileCommand command) {
    userValidator.validateId(veterinarianId);
    User user = findById(veterinarianId);
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPhone(command.getPhone());
    userValidator.validateForUpdate(user);
    userRepository.save(user);
  }

  @Override
  @Transactional
  public void deactivate(Long userId) {
    User user = findById(userId);
    if (removesLastActiveAdministrator(user, true, false)) {
      throw new BusinessRuleException(
          "The last active system administrator cannot be deactivated or lose its role");
    }
    user.setActive(false);
    userRepository.save(user);
    log.info("User deactivated [userId={}]", userId);
  }

  @Override
  public List<User> findActiveByWorkClinicId(Long clinicId) {
    return userRepository.findByWorkClinicId(clinicId);
  }

  @Override
  public List<User> findVeterinariansByClinicId(Long clinicId) {
    return userRepository.findByWorkClinicIdAndRoleAndActiveTrue(clinicId, Role.VETERINARIAN);
  }

  @Override
  public List<User> findReceptionistsByClinicId(Long clinicId) {
    return userRepository.findByWorkClinicIdAndRoleAndActiveTrue(clinicId, Role.RECEPTIONIST);
  }

  @Override
  @Transactional
  public User registerVeterinarian(RegisterVeterinarianCommand command) {
    User user = new User();
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPassword(command.getPassword());
    user.setPhone(command.getPhone());
    user.setRole(Role.VETERINARIAN);
    user.setActive(true);
    return save(user);
  }

  @Override
  @Transactional
  public User registerStaffForClinic(CreateStaffCommand command, VeterinaryClinic clinic) {
    User user = new User();
    user.setFirstName(command.getFirstName());
    user.setLastName(command.getLastName());
    user.setEmail(command.getEmail());
    user.setPassword(command.getPassword());
    user.setPhone(command.getPhone());
    user.setRole(command.getRole());
    user.setActive(true);
    user.setWorkClinic(clinic);
    return save(user);
  }

  private void encodePassword(User user) {
    user.setPassword(passwordEncoder.encode(user.getPassword()));
  }

  private void encodePassword(User user, String newPassword) {
    user.setPassword(passwordEncoder.encode(newPassword));
  }

  /**
   * Returns whether applying the change to {@code user} would leave the system without an active
   * system administrator.
   *
   * <p>Single enforcement point for the last-administrator invariant: every change to a user's
   * active state or role must go through {@code deactivate} or {@code updateComplete}. The
   * administrator count is only queried when {@code user} is an active administrator and the change
   * deactivates it or removes its role; a {@code null} {@code active} means "no change".
   */
  private boolean removesLastActiveAdministrator(
      User user, boolean deactivating, boolean losingRole) {
    if (user.getRole() != Role.SYSTEM_ADMINISTRATOR || (!deactivating && !losingRole)) {
      return false;
    }
    return userRepository.countByRoleAndActiveTrue(Role.SYSTEM_ADMINISTRATOR) <= 1;
  }
}
