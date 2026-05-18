(function () {
  document.addEventListener("DOMContentLoaded", function () {
    var rollBtn = document.getElementById("roll-btn");
    var resultText = document.getElementById("dice-result");
    var rollForm = document.getElementById("roll-form");

    // Lógica de animação das peças do tabuleiro
    var tokens = document.querySelectorAll(".token");
    var prevPositions = sessionStorage.getItem("tokenPositions");
    
    if (prevPositions) {
      try {
        prevPositions = JSON.parse(prevPositions);
        tokens.forEach(function(token, index) {
          if (prevPositions[index]) {
            // Guarda as posições alvo (novas)
            var targetLeft = token.style.left;
            var targetTop = token.style.top;
            
            // Define temporariamente para as posições antigas sem transição
            token.style.transition = "none";
            token.style.left = prevPositions[index].left;
            token.style.top = prevPositions[index].top;
            
            // Força reflow para o navegador aplicar as posições antigas
            void token.offsetWidth;
            
            // Restaura a transição e define as posições alvo
            token.style.transition = "left 1.2s ease-in-out, top 1.2s ease-in-out";
            token.style.left = targetLeft;
            token.style.top = targetTop;
          }
        });
      } catch (e) {
        console.error("Erro ao ler posições das peças:", e);
      }
      sessionStorage.removeItem("tokenPositions");
    } else {
      // Se não houver posições anteriores, garante a transição para movimentos futuros
      tokens.forEach(function(token) {
        token.style.transition = "left 1.2s ease-in-out, top 1.2s ease-in-out";
      });
    }

    if (!rollBtn || !resultText || !rollForm) return;

    rollBtn.addEventListener("click", function () {
      // Salva as posições atuais antes de rolar o dado
      var currentPositions = [];
      document.querySelectorAll(".token").forEach(function(token) {
        currentPositions.push({
          left: token.style.left,
          top: token.style.top
        });
      });
      sessionStorage.setItem("tokenPositions", JSON.stringify(currentPositions));

      // Exibe o status de rolagem com animação
      resultText.textContent = "Rolando...";
      resultText.classList.remove("dice-result-show");
      // Force reflow para reiniciar a animação
      void resultText.offsetWidth;
      resultText.classList.add("dice-result-show");

      // Animação rápida no botão
      rollBtn.classList.add("rolling");
      rollBtn.disabled = true;

      // Após a animação, submeter o formulário para o servidor processar a jogada e atualizar as posições
      setTimeout(function () {
        rollBtn.classList.remove("rolling");
        rollForm.submit();
      }, 800);
    });
  });
})();
