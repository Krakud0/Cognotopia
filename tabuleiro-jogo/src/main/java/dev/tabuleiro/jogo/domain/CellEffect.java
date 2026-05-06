package dev.tabuleiro.jogo.domain;

/**
 * Efeitos mecânicos da casa (Cognolopi e extensões).
 * Desafios textuais usam {@link BoardCell#challengeId}.
 */
public enum CellEffect {
    NONE,
    /** Avança N casas imediatamente (encadeamento). */
    SKIP_1,
    SKIP_2,
    SKIP_3,
    /** Recua N casas (reavalia a casa final). */
    BACK_1,
    BACK_2,
    BACK_3,
    /** A dupla atual perde a próxima vez que lhe tocar jogar. */
    MISS_TURN,
    /** Joga o dado outra vez após concluir a casa. */
    ROLL_AGAIN,
    /** Alias histórico — mesmo que {@link #ROLL_AGAIN}. */
    EXTRA_TURN,
    /** Todas as duplas recuam 3 casas. */
    ALL_BACK_3,
    /** Escolher uma dupla adversária para recuar 4 casas (UI dedicada). */
    CHOOSE_OPPONENT_BACK_4
}
