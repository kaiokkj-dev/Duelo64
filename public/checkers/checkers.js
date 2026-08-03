const TOKEN_STORAGE_KEY = "duelo64.accessToken";

const timeOptions = document.querySelectorAll(".time-option");
const quickPlayButton = document.querySelector("#quick-play");
const createPrivateRoomButton = document.querySelector("#create-private-room");
const privateRoomResult = document.querySelector("#private-room-result");
const generatedCodeElement = document.querySelector("#generated-code");
const copyRoomCodeButton = document.querySelector("#copy-room-code");
const enterCreatedRoomLink = document.querySelector("#enter-created-room");
const joinForm = document.querySelector("#join-form");
const roomCodeInput = document.querySelector("#room-code");
const joinMessage = document.querySelector("#join-message");

let selectedTime = 10;
let generatedRoomCode = "";

function isAuthenticated() {
  return Boolean(sessionStorage.getItem(TOKEN_STORAGE_KEY));
}

function requestAuthentication(destination) {
  const returnTo = encodeURIComponent(destination);
  window.location.href = `../entrar.html?returnTo=${returnTo}`;
}

function refreshIcons() {
  window.lucide?.createIcons();
}

function createRoomCode() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  return Array.from({ length: 6 }, () => alphabet[Math.floor(Math.random() * alphabet.length)]).join("");
}

timeOptions.forEach((option) => {
  option.addEventListener("click", () => {
    timeOptions.forEach((button) => button.classList.remove("active"));
    option.classList.add("active");
    selectedTime = Number(option.dataset.time);
  });
});

quickPlayButton?.addEventListener("click", () => {
  const destination = `checkers/room/index.html?mode=public&time=${selectedTime}`;

  if (!isAuthenticated()) {
    requestAuthentication(destination);
    return;
  }

  quickPlayButton.disabled = true;
  quickPlayButton.textContent = "Procurando adversário...";

  window.setTimeout(() => {
    window.location.href = `room/index.html?mode=public&time=${selectedTime}`;
  }, 900);
});

createPrivateRoomButton?.addEventListener("click", () => {
  if (!isAuthenticated()) {
    requestAuthentication("checkers/index.html?action=create-private");
    return;
  }

  generatedRoomCode = createRoomCode();
  generatedCodeElement.textContent = generatedRoomCode;
  enterCreatedRoomLink.href = `room/index.html?mode=private&code=${generatedRoomCode}&time=${selectedTime}`;
  privateRoomResult.hidden = false;
  privateRoomResult.scrollIntoView({ behavior: "smooth", block: "center" });
  refreshIcons();
});

copyRoomCodeButton?.addEventListener("click", async () => {
  if (!generatedRoomCode) return;
  await navigator.clipboard.writeText(generatedRoomCode);
  copyRoomCodeButton.innerHTML = '<i data-lucide="check"></i> Copiado';
  refreshIcons();
});

roomCodeInput?.addEventListener("input", () => {
  roomCodeInput.value = roomCodeInput.value.toUpperCase().replace(/[^A-Z0-9]/g, "");
  joinMessage.textContent = "";
});

joinForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  const code = roomCodeInput.value.trim();

  if (code.length !== 6) {
    joinMessage.textContent = "O código precisa ter 6 caracteres.";
    roomCodeInput.focus();
    return;
  }

  const destination = `checkers/room/index.html?mode=private&code=${encodeURIComponent(code)}&time=10`;

  if (!isAuthenticated()) {
    requestAuthentication(destination);
    return;
  }

  window.location.href = `room/index.html?mode=private&code=${encodeURIComponent(code)}&time=10`;
});

const pageAction = new URLSearchParams(window.location.search).get("action");

if (pageAction === "create-private" && isAuthenticated()) {
  window.history.replaceState({}, "", "index.html");
  window.setTimeout(() => createPrivateRoomButton?.click(), 150);
}

refreshIcons();
