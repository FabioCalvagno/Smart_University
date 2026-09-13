package it.unict.smartuniversity.examservice.repository;

import it.unict.smartuniversity.examservice.model.Prenotazione;
import it.unict.smartuniversity.examservice.model.ExamCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    List<Prenotazione> findByExamCall(ExamCall examCall);
    List<Prenotazione> findByUsername(String username);
    Optional<Prenotazione> findByExamCallAndMatricola(ExamCall examCall, String matricola);
}