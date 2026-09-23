package it.unict.smartuniversity.examservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Entity
@Table(name = "exam_calls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "insegnamento_id", nullable = false)
    private Insegnamento insegnamento; // L'appello fa riferimento a un insegnamento inserito dall'Admin

    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataEsame; // Formattazione corretta per la deserializzazione JSON

    @Column(nullable = false)
    private String docente; // Username del docente che tiene l'esame

    @Column(nullable = false)
    private boolean chiuso = false; // Flag per la chiusura dell'appello da parte del docente
}
