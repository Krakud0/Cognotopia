package dev.tabuleiro.jogo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChallengeDef(
        String id,
        String titulo,
        String instrucoes,
        List<HumanDimension> dimensoes,
        String imagem,
        Integer tempoSugeridoSeg,
        String notaEducador
) {
    public ChallengeDef {
        if (dimensoes == null) {
            dimensoes = List.of();
        }
    }
}
