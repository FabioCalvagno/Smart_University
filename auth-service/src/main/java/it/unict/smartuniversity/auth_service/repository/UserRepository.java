package it.unict.smartuniversity.auth_service.repository;

import it.unict.smartuniversity.auth_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Metodo per cercare un utente tramite username (utile per il login)
    Optional<User> findByUsername(String username);
}