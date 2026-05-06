package dev.tabuleiro.jogo.web;

import dev.tabuleiro.jogo.domain.BoardCell;
import dev.tabuleiro.jogo.domain.GameContent;
import dev.tabuleiro.jogo.domain.GamePhase;
import dev.tabuleiro.jogo.service.GameContentService;
import dev.tabuleiro.jogo.session.GameSessionState;
import dev.tabuleiro.jogo.session.TeamState;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class GameController {

    private final GameSessionState session;
    private final GameContentService contentService;

    public GameController(GameSessionState session, GameContentService contentService) {
        this.session = session;
        this.contentService = contentService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("phase", session.getPhase());
        return "index";
    }

    @PostMapping("/iniciar")
    public String iniciar(
            @RequestParam("numDuplas") int numDuplas,
            @RequestParam(value = "tipoParticipante", required = false) String tipoParticipante,
            @RequestParam(value = "nomeDupla1", required = false) String nomeDupla1,
            @RequestParam(value = "nomeDupla2", required = false) String nomeDupla2,
            @RequestParam(value = "nomeDupla3", required = false) String nomeDupla3,
            @RequestParam(value = "nomeDupla4", required = false) String nomeDupla4,
            RedirectAttributes redirectAttributes
    ) {
        GameContent content = contentService.getContent();
        String label = "Dupla";
        if (tipoParticipante != null && tipoParticipante.equalsIgnoreCase("JOGADOR")) {
            label = "Jogador";
        }
        List<String> names = new ArrayList<>();
        names.add(nomeDupla1);
        names.add(nomeDupla2);
        names.add(nomeDupla3);
        names.add(nomeDupla4);
        try {
            session.startGame(numDuplas, names, content, label);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("erro", e.getMessage());
            return "redirect:/";
        }
        return "redirect:/jogo";
    }

    @GetMapping("/jogo")
    public String jogo(Model model) {
        if (session.getPhase() == GamePhase.SETUP) {
            return "redirect:/";
        }
        GameContent content = contentService.getContent();
        List<BoardCell> cells = content.cells();
        List<TeamView> teamViews = new ArrayList<>();
        int max = Math.max(0, cells.size() - 1);
        for (int i = 0; i < session.getTeams().size(); i++) {
            TeamState t = session.getTeams().get(i);
            int idx = Math.min(Math.max(t.getPositionIndex(), 0), max);
            teamViews.add(new TeamView(t.getName(), i, cells.get(idx)));
        }
        model.addAttribute("session", session);
        model.addAttribute("board", content.board());
        model.addAttribute("teamViews", teamViews);
        if (session.isAwaitingChallenge()) {
            model.addAttribute("challenge", session.currentChallenge(content));
            model.addAttribute("pendingCell", session.pendingCell(content));
        } else {
            model.addAttribute("challenge", null);
            model.addAttribute("pendingCell", null);
        }
        return "game";
    }

    @PostMapping("/jogo/rolar")
    public String rolar(RedirectAttributes redirectAttributes) {
        GameContent content = contentService.getContent();
        String err = session.rollDice(content);
        if (err != null) {
            redirectAttributes.addFlashAttribute("erro", err);
        }
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/desafio/concluir")
    public String concluirDesafio() {
        session.completeChallenge();
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/acao/passar")
    public String passar() {
        session.passTurnOnYellow();
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/acao/aceitar")
    public String aceitar() {
        session.acceptGreen();
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/acao/recusar")
    public String recusar() {
        session.rejectGreen();
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/acao/trocar")
    public String trocar() {
        GameContent content = contentService.getContent();
        session.swapOrange(content);
        return "redirect:/jogo";
    }

    @PostMapping("/jogo/adversario")
    public String escolherAdversario(
            @RequestParam("indiceDupla") int indiceDupla,
            RedirectAttributes redirectAttributes
    ) {
        String err = session.applyOpponentBack(indiceDupla);
        if (err != null) {
            redirectAttributes.addFlashAttribute("erro", err);
        }
        return "redirect:/jogo";
    }

    @PostMapping("/nova-partida")
    public String novaPartida() {
        session.reset();
        return "redirect:/";
    }
}
