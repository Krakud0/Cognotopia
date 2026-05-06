package dev.tabuleiro.jogo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BoardCell(
        String id,
        CellType type,
        CellColor color,
        String challengeId,
        CellEffect effect,
        double leftPct,
        double topPct
) {
    public BoardCell {
        if (effect == null) {
            effect = CellEffect.NONE;
        }
        if (type == null) {
            type = CellType.NORMAL;
        }
        if (color == null) {
            color = CellColor.AMARELO;
        }
    }
}
