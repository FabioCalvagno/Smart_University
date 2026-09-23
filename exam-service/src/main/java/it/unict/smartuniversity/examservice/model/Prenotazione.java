package it.unict.smartuniversity.examservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prenotazioni", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_call_id", "matricola"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_call_id", nullable = false)
    @JsonIgnoreProperties("prenotazioni") // <-- EVITA IL LOOP INFINITO DI SERIALIZZAZIONE JSON (FIX ERRORE 500)
    private ExamCall examCall;

    @Column(nullable = false)
    private String matricola;

    @Column(nullable = false)
    private String username;

    @Builder.Default
    @Column(nullable = false)
    private boolean verbalizzata = false;

    @Builder.Default
    @Column(nullable = false)
    private String stato = "ATTESA_VERBALIZZAZIONE"; // Valori possibili: ATTESA_VERBALIZZAZIONE, RIMANDATO, VERBALIZZATO
}
