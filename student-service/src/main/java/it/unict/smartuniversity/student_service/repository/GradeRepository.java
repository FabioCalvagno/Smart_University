package it.unict.smartuniversity.student_service.repository;

import it.unict.smartuniversity.student_service.model.Grade;
import it.unict.smartuniversity.student_service.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    // Questo metodo permette di cercare tutti i voti appartenenti a un determinato studente
    List<Grade> findByStudent(Student student);
    boolean existsByStudentAndNomeInsegnamento(Student student, String nomeInsegnamento);
}
