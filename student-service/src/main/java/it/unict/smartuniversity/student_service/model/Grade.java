package it.unict.smartuniversity.student_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "student_grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeInsegnamento;

    @Column(nullable = false)
    private int voto;

    @Column(nullable = false)
    private boolean lode;

    @Column(nullable = false)
    private String docente;

    // Relazione con lo studente: ogni voto appartiene a uno specifico studente
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
}