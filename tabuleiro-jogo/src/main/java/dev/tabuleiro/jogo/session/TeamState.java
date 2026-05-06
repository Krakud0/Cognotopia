package dev.tabuleiro.jogo.session;

import java.io.Serializable;

public class TeamState implements Serializable {

    private String name;
    private int positionIndex;
    /** Quando true, na próxima vez que for a vez desta dupla, não se rola o dado — passa a vez. */
    private boolean skipNextTurn;

    public TeamState() {
    }

    public TeamState(String name, int positionIndex) {
        this.name = name;
        this.positionIndex = positionIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public void setPositionIndex(int positionIndex) {
        this.positionIndex = positionIndex;
    }

    public boolean isSkipNextTurn() {
        return skipNextTurn;
    }

    public void setSkipNextTurn(boolean skipNextTurn) {
        this.skipNextTurn = skipNextTurn;
    }
}
