const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const API_BASE_URL = "http://localhost:8080/api/v1";

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

function getToken() {
  return sessionStorage.getItem(TOKEN_STORAGE_KEY);
}

async function apiRequest(path, options = {}) {
  const token = getToken();

  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        ...options.headers,
      },
    });
  } catch {
    throw new Error("Nao foi possivel conectar ao servidor. Verifique se o backend esta ligado.");
  }

  const contentType = response.headers.get("content-type") || "";
  const responseBody = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    const error = new Error(responseBody?.message || "Nao foi possivel concluir a acao.");
    error.code = responseBody?.code;
    error.status = response.status;
    throw error;
  }

  return responseBody;
}

function handleUnauthorized(destination) {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  requestAuthentication(destination);
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

createPrivateRoomButton?.addEventListener("click", async () => {
  if (!isAuthenticated()) {
    requestAuthentication("checkers/index.html?action=create-private");
    return;
  }

  createPrivateRoomButton.disabled = true;
  createPrivateRoomButton.innerHTML = '<i data-lucide="loader-circle"></i> Criando sala';
  refreshIcons();

  try {
    const room = await apiRequest("/checkers/rooms", {
      method: "POST",
      body: JSON.stringify({ timeControlMinutes: selectedTime }),
    });

    generatedRoomCode = room.code;
    generatedCodeElement.textContent = generatedRoomCode;
    enterCreatedRoomLink.href = `room/index.html?mode=private&code=${generatedRoomCode}&time=${room.timeControlMinutes}`;
    privateRoomResult.hidden = false;
    privateRoomResult.scrollIntoView({ behavior: "smooth", block: "center" });
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      handleUnauthorized("checkers/index.html?action=create-private");
      return;
    }

    joinMessage.textContent = error.message;
  } finally {
    createPrivateRoomButton.disabled = false;
    createPrivateRoomButton.innerHTML = '<i data-lucide="lock"></i> Criar sala privada';
    refreshIcons();
  }
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

joinForm?.addEventListener("submit", async (event) => {
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

  const submitButton = joinForm.querySelector("button[type='submit']");
  submitButton.disabled = true;
  submitButton.innerHTML = '<i data-lucide="loader-circle"></i> Entrando';
  refreshIcons();

  try {
    const room = await apiRequest(`/checkers/rooms/${encodeURIComponent(code)}/join`, {
      method: "POST",
    });

    window.location.href = `room/index.html?mode=private&code=${encodeURIComponent(room.code)}&time=${room.timeControlMinutes}`;
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      handleUnauthorized(destination);
      return;
    }

    joinMessage.textContent = error.message;
    roomCodeInput.focus();
  } finally {
    submitButton.disabled = false;
    submitButton.innerHTML = '<i data-lucide="log-in"></i> Entrar na sala';
    refreshIcons();
  }
});

const pageAction = new URLSearchParams(window.location.search).get("action");

if (pageAction === "create-private" && isAuthenticated()) {
  window.history.replaceState({}, "", "index.html");
  window.setTimeout(() => createPrivateRoomButton?.click(), 150);
}

refreshIcons();
