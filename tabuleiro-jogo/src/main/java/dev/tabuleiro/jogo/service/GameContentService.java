package dev.tabuleiro.jogo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.tabuleiro.jogo.domain.BoardDefinition;
import dev.tabuleiro.jogo.domain.ChallengesFile;
import dev.tabuleiro.jogo.domain.GameContent;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

@Service
public class GameContentService {

    private final ObjectMapper objectMapper;
    private GameContent content;

    public GameContentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        try {
            BoardDefinition board;
            try (InputStream in = new ClassPathResource("board.json").getInputStream()) {
                board = objectMapper.readValue(in, BoardDefinition.class);
            }
            ChallengesFile challengesFile;
            try (InputStream in = new ClassPathResource("challenges.json").getInputStream()) {
                challengesFile = objectMapper.readValue(in, ChallengesFile.class);
            }
            this.content = GameContent.merge(board, challengesFile.challenges());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar board.json ou challenges.json", e);
        }
    }

    public GameContent getContent() {
        return content;
    }
}
