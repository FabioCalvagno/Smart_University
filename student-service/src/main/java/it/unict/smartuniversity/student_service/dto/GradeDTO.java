package it.unict.smartuniversity.student_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nomeInsegnamento;
    private Integer voto;
    private Boolean lode;
    private String docente;
}