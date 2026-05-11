(function () {
  document.addEventListener("DOMContentLoaded", function () {
    var rollBtn = document.getElementById("roll-btn");
    var resultText = document.getElementById("dice-result");

    if (!rollBtn || !resultText) return;

    rollBtn.addEventListener("click", function () {
      // Gera número aleatório de 1 a 6
      var result = Math.floor(Math.random() * 6) + 1;

      // Exibe o resultado
      resultText.textContent = "Ande " + result + " casas!";
      resultText.classList.add("dice-result-show");

      // Animação rápida no botão
      rollBtn.classList.add("rolling");
      setTimeout(function () {
        rollBtn.classList.remove("rolling");
      }, 400);
    });
  });
})();
