// Comportamento compartilhado pelas páginas institucionais.
const header = document.querySelector(".site-header");
const menuButton = document.querySelector(".menu-button");

if (window.lucide) window.lucide.createIcons();

function closeMenu() {
  header?.classList.remove("menu-open");
  menuButton?.setAttribute("aria-expanded", "false");
}

menuButton?.addEventListener("click", () => {
  const isOpen = header?.classList.toggle("menu-open") ?? false;
  menuButton.setAttribute("aria-expanded", String(isOpen));
});

document.querySelectorAll(".top-navigation a").forEach((link) => {
  link.addEventListener("click", closeMenu);
});

document.addEventListener("click", (event) => {
  if (header && !header.contains(event.target)) closeMenu();
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeMenu();
    menuButton?.focus();
  }
});
