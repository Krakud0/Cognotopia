package dev.tabuleiro.jogo.web;

import dev.tabuleiro.jogo.domain.BoardCell;

public record TeamView(String name, int colorIndex, BoardCell cell) {
}
