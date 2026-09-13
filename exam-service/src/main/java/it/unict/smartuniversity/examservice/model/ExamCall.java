package it.unict.smartuniversity.examservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
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
    private LocalDate dataEsame;

    @Column(nullable = false)
    private String docente; // Username del docente che tiene l'esame (ereditato dal corso)
}