package dev.tabuleiro.jogo.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChallengesFile(List<ChallengeDef> challenges) {
    public ChallengesFile {
        if (challenges == null) {
            challenges = List.of();
        }
    }
}
