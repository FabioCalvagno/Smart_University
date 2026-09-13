package it.unict.smartuniversity.examservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeEvent implements Serializable {
    
    private String matricola;        // Identificativo unico dello studente
    private String nomeInsegnamento; // Nome dell'esame verbalizzato (es. "Ingegneria dei Sistemi Distribuiti")
    private int voto;                // Voto finale (es. 18-30)
    private boolean lode;            // Se presente la lode (voto = 30)
    private String docente;          // Username del docente che ha firmato il verbale
}