package it.unict.smartuniversity.student_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeEvent {
    private String matricola;
    private String nomeInsegnamento;
    private int voto;
    private boolean lode;
    private String docente;
}