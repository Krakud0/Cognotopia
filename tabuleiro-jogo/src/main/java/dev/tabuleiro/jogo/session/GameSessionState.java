package dev.tabuleiro.jogo.session;

import dev.tabuleiro.jogo.domain.BoardCell;
import dev.tabuleiro.jogo.domain.CellColor;
import dev.tabuleiro.jogo.domain.CellEffect;
import dev.tabuleiro.jogo.domain.ChallengeDef;
import dev.tabuleiro.jogo.domain.GameContent;
import dev.tabuleiro.jogo.domain.GamePhase;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GameSessionState implements Serializable {

    private GamePhase phase = GamePhase.SETUP;
    private String participantLabel = "Dupla";
    private final List<TeamState> teams = new ArrayList<>();
    private int currentTeamIndex;
    private Integer lastRoll;
    private boolean awaitingChallenge;
    /** Desafio ligado a uma casa já “deixada” (ex.: após ALL_BACK_3). */
    private String pendingChallengeId;
    private boolean grantExtraTurn;
    private boolean awaitingOpponentSelection;

    /** Índice da casa que disparou o pop-up atual. */
    private Integer pendingCellIndex;

    /**
     * Para cada casa (índice), mantemos um "deck" embaralhado por cor para
     * evitar repetir o mesmo desafio quando alguém parar nessa casa de novo.
     */
    private final Map<Integer, List<String>> deckByCellIndex = new HashMap<>();
    private final Map<Integer, Integer> deckPosByCellIndex = new HashMap<>();

    public void reset() {
        phase = GamePhase.SETUP;
        participantLabel = "Dupla";
        teams.clear();
        currentTeamIndex = 0;
        lastRoll = null;
        awaitingChallenge = false;
        pendingChallengeId = null;
        grantExtraTurn = false;
        awaitingOpponentSelection = false;
        pendingCellIndex = null;
        deckByCellIndex.clear();
        deckPosByCellIndex.clear();
    }

    public void startGame(int teamCount, List<String> names, GameContent content) {
        startGame(teamCount, names, content, "Dupla");
    }

    public void startGame(int teamCount, List<String> names, GameContent content, String participantLabel) {
        if (teamCount < 2 || teamCount > 4) {
            throw new IllegalArgumentException("Número de duplas deve ser entre 2 e 4.");
        }
        this.participantLabel = (participantLabel == null || participantLabel.isBlank()) ? "Dupla" : participantLabel.trim();
        List<BoardCell> cells = content.cells();
        if (cells.isEmpty()) {
            throw new IllegalStateException("Tabuleiro sem casas.");
        }
        teams.clear();
        for (int i = 0; i < teamCount; i++) {
            String n = (names != null && i < names.size() && names.get(i) != null && !names.get(i).isBlank())
                    ? names.get(i).trim()
                    : this.participantLabel + " " + (i + 1);
            teams.add(new TeamState(n, 0));
        }
        currentTeamIndex = 0;
        lastRoll = null;
        awaitingChallenge = false;
        pendingChallengeId = null;
        grantExtraTurn = false;
        awaitingOpponentSelection = false;
        pendingCellIndex = null;
        deckByCellIndex.clear();
        deckPosByCellIndex.clear();
        phase = GamePhase.PLAYING;
    }

    public String getParticipantLabel() {
        return participantLabel;
    }

    public String rollDice(GameContent content) {
        if (phase != GamePhase.PLAYING) {
            return "Não há partida em curso.";
        }
        if (awaitingChallenge || awaitingOpponentSelection) {
            return "Terminem a ação desta casa antes de rolar o dado.";
        }
        TeamState current = teams.get(currentTeamIndex);
        if (current.isSkipNextTurn()) {
            current.setSkipNextTurn(false);
            lastRoll = null;
            advanceTurnSkipExtra();
            return null;
        }

        List<BoardCell> cells = content.cells();
        int lastIdx = cells.size() - 1;
        int roll = ThreadLocalRandom.current().nextInt(1, 7);
        lastRoll = roll;

        int pos = Math.min(current.getPositionIndex() + roll, lastIdx);
        resolvePositionAfterRoll(content, current, pos, lastIdx);

        if (phase == GamePhase.FINISHED) {
            awaitingChallenge = false;
            pendingChallengeId = null;
            awaitingOpponentSelection = false;
            return null;
        }
        if (awaitingOpponentSelection) {
            return null;
        }

        queueLandingAction(content);
        return null;
    }

    private void queueLandingAction(GameContent content) {
        pendingChallengeId = null;
        pendingCellIndex = null;

        BoardCell cell = currentCell(content);
        if (cell == null) {
            finishTurnOrStayForExtraRoll();
            return;
        }

        pendingCellIndex = getCurrentTeam().getPositionIndex();

        // Amarelo: sem carta → pop-up só para passar a vez.
        if (cell.color() == CellColor.AMARELO) {
            awaitingChallenge = true;
            return;
        }

        // Vermelho: efeitos são mecânicos e já foram aplicados no resolver.
        // Se pararmos numa vermelha sem efeito (ou após cadeia), tratamos como "apenas continuar".
        if (cell.color() == CellColor.VERMELHO) {
            awaitingChallenge = true;
            return;
        }

        // Verde/Laranja/Marrom: sorteio sem repetir dentro da MESMA casa.
        pendingChallengeId = drawNextChallengeIdForCell(content, pendingCellIndex, cell.color());
        awaitingChallenge = true;
    }

    private String drawNextChallengeIdForCell(GameContent content, int cellIndex, CellColor color) {
        List<String> pool = content.challengeIdsByColor(color);
        if (pool.isEmpty()) {
            return null;
        }
        List<String> deck = deckByCellIndex.get(cellIndex);
        Integer pos = deckPosByCellIndex.get(cellIndex);
        if (deck == null || pos == null || pos >= deck.size()) {
            deck = new ArrayList<>(pool);
            Collections.shuffle(deck, ThreadLocalRandom.current());
            deckByCellIndex.put(cellIndex, deck);
            pos = 0;
        }
        String id = deck.get(pos);
        deckPosByCellIndex.put(cellIndex, pos + 1);
        return id;
    }

    /**
     * Após o dado: aplica saltos, recuos e efeitos especiais até estabilizar ou pedir input.
     */
    private void resolvePositionAfterRoll(GameContent content, TeamState current, int pos, int lastIdx) {
        List<BoardCell> cells = content.cells();
        pendingChallengeId = null;
        pendingCellIndex = null;
        int iterations = 0;

        while (iterations++ < 40) {
            if (pos >= lastIdx) {
                current.setPositionIndex(lastIdx);
                phase = GamePhase.FINISHED;
                return;
            }

            BoardCell cell = cells.get(pos);
            CellEffect e = cell.effect();

            if (e == CellEffect.ALL_BACK_3) {
                for (TeamState t : teams) {
                    t.setPositionIndex(Math.max(0, t.getPositionIndex() - 3));
                }
                pos = Math.max(0, pos - 3);
                current.setPositionIndex(pos);
                queueChallengeIfPresent(content, cell);
                return;
            }

            if (e == CellEffect.CHOOSE_OPPONENT_BACK_4) {
                current.setPositionIndex(pos);
                awaitingOpponentSelection = true;
                return;
            }

            switch (e) {
                case SKIP_1 -> pos = Math.min(pos + 1, lastIdx);
                case SKIP_2 -> pos = Math.min(pos + 2, lastIdx);
                case SKIP_3 -> pos = Math.min(pos + 3, lastIdx);
                case BACK_1 -> pos = Math.max(0, pos - 1);
                case BACK_2 -> pos = Math.max(0, pos - 2);
                case BACK_3 -> pos = Math.max(0, pos - 3);
                case MISS_TURN -> {
                    current.setSkipNextTurn(true);
                    current.setPositionIndex(pos);
                    grantExtraTurn = false;
                    return;
                }
                case ROLL_AGAIN, EXTRA_TURN -> {
                    grantExtraTurn = true;
                    current.setPositionIndex(pos);
                    return;
                }
                case NONE -> {
                    current.setPositionIndex(pos);
                    return;
                }
                default -> {
                    current.setPositionIndex(pos);
                    return;
                }
            }

            if (pos >= lastIdx) {
                current.setPositionIndex(lastIdx);
                phase = GamePhase.FINISHED;
                return;
            }
        }

        current.setPositionIndex(Math.min(Math.max(pos, 0), lastIdx));
    }

    private void queueChallengeIfPresent(GameContent content, BoardCell triggerCell) {
        String id = triggerCell.challengeId();
        if (id != null && !id.isBlank() && content.challenge(id) != null) {
            pendingChallengeId = id;
            awaitingChallenge = true;
        }
    }

    private ChallengeDef resolveActiveChallenge(GameContent content) {
        if (pendingChallengeId != null) {
            return content.challenge(pendingChallengeId);
        }
        BoardCell c = currentCell(content);
        if (c == null || c.challengeId() == null || c.challengeId().isBlank()) {
            return null;
        }
        return content.challenge(c.challengeId());
    }

    public void completeChallenge() {
        if (!awaitingChallenge) {
            return;
        }
        awaitingChallenge = false;
        pendingChallengeId = null;
        pendingCellIndex = null;
        if (phase == GamePhase.FINISHED) {
            return;
        }
        finishTurnOrStayForExtraRoll();
    }

    public void passTurnOnYellow() {
        if (!awaitingChallenge) {
            return;
        }
        awaitingChallenge = false;
        pendingChallengeId = null;
        pendingCellIndex = null;
        finishTurnOrStayForExtraRoll();
    }

    public void acceptGreen() {
        completeChallenge();
    }

    public void rejectGreen() {
        TeamState current = getCurrentTeam();
        if (current != null) {
            current.setSkipNextTurn(true);
        }
        completeChallenge();
    }

    public void swapOrange(GameContent content) {
        if (!awaitingChallenge || pendingCellIndex == null) {
            return;
        }
        BoardCell cell = pendingCell(content);
        if (cell == null || cell.color() != CellColor.LARANJA) {
            return;
        }
        pendingChallengeId = drawNextChallengeIdForCell(content, pendingCellIndex, CellColor.LARANJA);
    }

    public String applyOpponentBack(int targetTeamIndex) {
        if (!awaitingOpponentSelection) {
            return "Não há escolha de adversário pendente.";
        }
        if (targetTeamIndex < 0 || targetTeamIndex >= teams.size() || targetTeamIndex == currentTeamIndex) {
            return "Escolham uma dupla adversária válida.";
        }
        TeamState target = teams.get(targetTeamIndex);
        target.setPositionIndex(Math.max(0, target.getPositionIndex() - 4));
        awaitingOpponentSelection = false;
        finishTurnOrStayForExtraRoll();
        return null;
    }

    private void finishTurnOrStayForExtraRoll() {
        if (phase != GamePhase.PLAYING) {
            return;
        }
        if (grantExtraTurn) {
            grantExtraTurn = false;
            return;
        }
        currentTeamIndex = (currentTeamIndex + 1) % teams.size();
    }

    private void advanceTurnSkipExtra() {
        currentTeamIndex = (currentTeamIndex + 1) % teams.size();
    }

    public GamePhase getPhase() {
        return phase;
    }

    public List<TeamState> getTeams() {
        return teams;
    }

    public int getCurrentTeamIndex() {
        return currentTeamIndex;
    }

    public TeamState getCurrentTeam() {
        return teams.isEmpty() ? null : teams.get(currentTeamIndex);
    }

    public Integer getLastRoll() {
        return lastRoll;
    }

    public boolean isAwaitingChallenge() {
        return awaitingChallenge;
    }

    public boolean isAwaitingOpponentSelection() {
        return awaitingOpponentSelection;
    }

    public String getPendingChallengeId() {
        return pendingChallengeId;
    }

    public BoardCell currentCell(GameContent content) {
        TeamState t = getCurrentTeam();
        if (t == null || content.cells().isEmpty()) {
            return null;
        }
        int idx = Math.min(Math.max(t.getPositionIndex(), 0), content.cells().size() - 1);
        return content.cells().get(idx);
    }

    public ChallengeDef currentChallenge(GameContent content) {
        if (pendingChallengeId != null) {
            return content.challenge(pendingChallengeId);
        }
        BoardCell c = currentCell(content);
        if (c == null || c.challengeId() == null) {
            return null;
        }
        return content.challenge(c.challengeId());
    }

    public BoardCell pendingCell(GameContent content) {
        if (pendingCellIndex == null || pendingCellIndex < 0 || pendingCellIndex >= content.cells().size()) {
            return currentCell(content);
        }
        return content.cells().get(pendingCellIndex);
    }

    public Integer getPendingCellIndex() {
        return pendingCellIndex;
    }
}
