# Cognotopia (MVP web)

Jogo de tabuleiro em **Spring Boot 3** + **Thymeleaf**: mesma sessão no navegador, duplas, dado 1–6, casas do tabuleiro físico **Cognotopia** (40 casas: COMEÇO → FIM).

## Executar

Requer Java 17+ e Maven.

```bash
mvn spring-boot:run
```

Abrir `http://localhost:8080`.

## Conteúdo editável

- [`src/main/resources/board.json`](src/main/resources/board.json) — ordem das casas, `effect` (`SKIP_1`, `BACK_3`, `MISS_TURN`, `ROLL_AGAIN`, `ALL_BACK_3`, `CHOOSE_OPPONENT_BACK_4`, …), posição dos pinos (`leftPct` / `topPct` em %).
- [`src/main/resources/challenges.json`](src/main/resources/challenges.json) — charadas e desafios físicos.
- Imagem de fundo: coloca a fotografia do tabuleiro em `src/main/resources/static/images/` (por exemplo `cognolopi-board.png`) e atualiza `backgroundImage` em `board.json`. Enquanto não houver foto, usa-se `board-bg.svg` como referência.

## Regras implementadas (resumo)

- **Pule N casas** — avanço imediato em cadeia.
- **Volte N casas** — recuo e nova avaliação da casa.
- **Passe 1 rodada sem jogar** — na próxima vez que for a vez dessa dupla, o dado não é rolado e a vez passa.
- **Jogue o dado novamente** — mesma dupla joga outra vez após concluir a casa.
- **Todos voltam 3 casas** — todas as duplas recuam 3; segue carta explicativa se existir em `challenges.json`.
- **Escolha 1 adversário para voltar 4 casas** — diálogo com botões por dupla.
