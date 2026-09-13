package it.unict.smartuniversity.examservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "insegnamenti")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Insegnamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codice; // es. INF-01

    @Column(nullable = false)
    private String nome; // es. Ingegneria dei Sistemi Distribuiti

    @Column(nullable = false)
    private String docente; // Username del docente responsabile (es. prof.tramontana)
}