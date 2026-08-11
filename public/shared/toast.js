(function initializeDueloToast(global) {
  const TYPES = new Set(["error", "warning", "success", "info"]);
  const recentMessages = new Map();

  function container() {
    let element = document.querySelector("#duelo-toast-container");
    if (element) return element;
    element = document.createElement("section");
    element.id = "duelo-toast-container";
    element.className = "duelo-toast-container";
    element.setAttribute("aria-label", "Notificações");
    document.body.appendChild(element);
    return element;
  }

  function dismiss(toast) {
    if (!toast || toast.classList.contains("leaving")) return;
    toast.classList.add("leaving");
    window.setTimeout(() => toast.remove(), 180);
  }

  function showToast(message, type = "info", options = {}) {
    const text = String(message || "").trim();
    if (!text) return null;
    const safeType = TYPES.has(type) ? type : "info";
    const dedupeKey = `${safeType}:${text}`;
    const now = Date.now();
    if (now - (recentMessages.get(dedupeKey) || 0) < 1200) return null;
    recentMessages.set(dedupeKey, now);

    const toast = document.createElement("article");
    toast.className = `duelo-toast ${safeType}`;
    toast.setAttribute("role", safeType === "error" ? "alert" : "status");

    const marker = document.createElement("span");
    marker.className = "duelo-toast-marker";
    marker.setAttribute("aria-hidden", "true");

    const content = document.createElement("p");
    content.textContent = text;

    const close = document.createElement("button");
    close.type = "button";
    close.className = "duelo-toast-close";
    close.setAttribute("aria-label", "Fechar notificação");
    close.textContent = "×";
    close.addEventListener("click", () => dismiss(toast));

    toast.append(marker, content, close);
    container().appendChild(toast);
    const duration = Number.isFinite(options.duration) ? options.duration : safeType === "error" ? 5200 : 3800;
    if (duration > 0) window.setTimeout(() => dismiss(toast), duration);
    return toast;
  }

  function friendlyError(error, fallback = "Não foi possível concluir a ação.") {
    const raw = String(error?.message || "").trim();
    if (!raw || /(?:exception|java\.|stack trace|at com\.)/i.test(raw) || raw.includes("\n")) return fallback;
    const normalized = raw.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    const mappings = [
      ["captura obrigatoria", "Captura obrigatória."],
      ["mesma peca deve continuar", "Você deve continuar a captura com esta peça."],
      ["lei da maioria", "Essa jogada não respeita a Lei da Maioria."],
      ["maior quantidade", "Essa jogada não respeita a Lei da Maioria."],
      ["proprio rei em xeque", "Essa jogada deixaria seu rei em xeque."],
      ["ainda nao e sua vez", "Não é sua vez."],
      ["nao e a vez", "Não é sua vez."],
      ["escolha queen", "Escolha uma peça para promoção."],
    ];
    return mappings.find(([key]) => normalized.includes(key))?.[1] || raw;
  }

  global.showToast = showToast;
  global.DueloToast = { show: showToast, errorMessage: friendlyError };
})(window);
