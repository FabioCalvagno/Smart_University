package it.unict.smartuniversity.student_service.repository;

import it.unict.smartuniversity.student_service.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    // Trova uno studente tramite il suo username (collegato all'utente di auth-service)
    Optional<Student> findByUsername(String username);
}