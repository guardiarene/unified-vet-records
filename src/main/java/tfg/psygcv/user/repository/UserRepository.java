package tfg.psygcv.user.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tfg.psygcv.user.entity.Role;
import tfg.psygcv.user.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmailAndActiveTrue(String email);

  @EntityGraph(attributePaths = {"clinicsOwned", "workClinic"})
  @Query("SELECT DISTINCT u FROM User u WHERE u.id = :userId")
  Optional<User> findByIdWithClinicContext(@Param("userId") Long userId);

  List<User> findByRoleAndActive(Role role, Boolean active);

  List<User> findByWorkClinicId(Long clinicId);

  List<User> findByWorkClinicIdAndRoleAndActiveTrue(Long clinicId, Role role);

  long countByRoleAndActiveTrue(Role role);
}
