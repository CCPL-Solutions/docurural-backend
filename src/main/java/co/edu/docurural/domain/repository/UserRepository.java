package co.edu.docurural.domain.repository;

import co.edu.docurural.domain.entity.User;
import co.edu.docurural.domain.enums.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatus(String email, UserStatus status);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
