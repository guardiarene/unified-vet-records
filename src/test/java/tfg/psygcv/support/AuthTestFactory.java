package tfg.psygcv.support;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import tfg.psygcv.config.security.AuthenticatedUser;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;

/**
 * Builds real {@link User} instances and their {@link Authentication} principal for unit tests.
 *
 * <p>The {@link AuthenticatedUser} principal is built with {@link AuthenticatedUser#from(User)} and
 * wrapped in an authenticated {@link UsernamePasswordAuthenticationToken}, mirroring what Spring
 * Security provides at runtime. Reused by tests that need an authenticated principal.
 */
public final class AuthTestFactory {

  private AuthTestFactory() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static User user(Long id, Role role) {
    User user = new User();
    user.setId(id);
    user.setEmail("user" + id + "@test.com");
    user.setPassword("encoded-password");
    user.setFirstName("First");
    user.setLastName("Last");
    user.setPhone("123456789");
    user.setRole(role);
    user.setActive(true);
    return user;
  }

  public static Authentication authentication(User user) {
    AuthenticatedUser principal = AuthenticatedUser.from(user);
    return UsernamePasswordAuthenticationToken.authenticated(
        principal, user.getPassword(), principal.getAuthorities());
  }
}
