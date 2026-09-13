package it.unict.smartuniversity.examservice.repository;

import it.unict.smartuniversity.examservice.model.Insegnamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface InsegnamentoRepository extends JpaRepository<Insegnamento, Long> {
    Optional<Insegnamento> findByCodice(String codice);
    List<Insegnamento> findByDocente(String docente);
}