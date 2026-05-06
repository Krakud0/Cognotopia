package dev.tabuleiro.jogo.domain;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.stream.Collectors;

public record GameContent(BoardDefinition board, Map<String, ChallengeDef> challengesById) {

    public static GameContent merge(BoardDefinition board, List<ChallengeDef> challenges) {
        Map<String, ChallengeDef> map = challenges.stream()
                .collect(Collectors.toMap(ChallengeDef::id, Function.identity(), (a, b) -> a));
        return new GameContent(board, Map.copyOf(map));
    }

    public List<BoardCell> cells() {
        return board.cells();
    }

    public ChallengeDef challenge(String id) {
        return id == null ? null : challengesById.get(id);
    }

    public List<String> challengeIdsByColor(CellColor color) {
        if (color == null) {
            return List.of();
        }
        String prefix = switch (color) {
            case VERDE -> "verde-";
            case LARANJA -> "laranja-";
            case MARROM -> "marrom-";
            default -> null;
        };
        if (prefix == null) {
            return List.of();
        }
        return challengesById.keySet().stream()
                .filter(Objects::nonNull)
                .filter(id -> id.startsWith(prefix))
                .sorted()
                .toList();
    }
}
