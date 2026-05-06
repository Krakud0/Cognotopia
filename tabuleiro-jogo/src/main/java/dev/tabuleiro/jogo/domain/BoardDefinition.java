package dev.tabuleiro.jogo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BoardDefinition(
        String backgroundImage,
        List<BoardCell> cells
) {
    public BoardDefinition {
        if (backgroundImage == null || backgroundImage.isBlank()) {
            backgroundImage = "/images/board-bg.svg";
        }
        if (cells == null) {
            cells = List.of();
        }
    }
}
