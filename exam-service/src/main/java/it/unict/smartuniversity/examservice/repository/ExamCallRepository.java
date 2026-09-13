package it.unict.smartuniversity.examservice.repository;

import it.unict.smartuniversity.examservice.model.ExamCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamCallRepository extends JpaRepository<ExamCall, Long> {
    // Trova tutti gli appelli d'esame associati a uno specifico docente
    List<ExamCall> findByDocente(String docente);
}