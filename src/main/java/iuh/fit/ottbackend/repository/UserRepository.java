package iuh.fit.ottbackend.repository;

import iuh.fit.ottbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);
    Optional<User> findByGoogleId(String googleId);
}
