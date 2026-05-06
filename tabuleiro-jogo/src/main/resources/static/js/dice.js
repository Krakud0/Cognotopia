(function () {
  document.addEventListener("DOMContentLoaded", function () {
    var dice = document.getElementById("dice");
    if (!dice) return;
    var roll = dice.getAttribute("data-last-roll");
    if (roll && roll !== "") {
      dice.classList.add("spin");
      window.setTimeout(function () {
        dice.classList.remove("spin");
      }, 650);
    }
  });
})();
