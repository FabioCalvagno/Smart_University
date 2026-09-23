package it.unict.smartuniversity.web_portal.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GradeDTO {
    private String nomeInsegnamento;
    private Integer voto;
    private Boolean lode;
    private String docente;
}