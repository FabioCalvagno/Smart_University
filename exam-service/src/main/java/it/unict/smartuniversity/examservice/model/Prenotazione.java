package it.unict.smartuniversity.examservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "prenotazioni", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_call_id", "matricola"}) // Evita doppie prenotazioni dello stesso studente allo stesso appello
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prenotazione {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_call_id", nullable = false)
    private ExamCall examCall;

    @Column(nullable = false)
    private String matricola; // Matricola dello studente prenotato

    @Column(nullable = false)
    private String username; // Username dello studente prenotato
}