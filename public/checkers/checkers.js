const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const { API_BASE_URL, WS_BASE_URL } = window.Duelo64Config;

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
const matchmakingMessage = document.querySelector("#matchmaking-message");

let selectedTime = 10;
let generatedRoomCode = "";
let matchmakingClient = null;
let matchmakingSubscription = null;
let matchmakingStatus = "IDLE";
let isChangingQueue = false;
let isNavigatingToMatch = false;

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

  const rawBody = await response.text();
  let responseBody = null;
  if (rawBody) {
    try {
      responseBody = JSON.parse(rawBody);
    } catch {
      responseBody = null;
    }
  }

  if (!response.ok) {
    const responseMessage = responseBody?.message
      || responseBody?.detail
      || responseBody?.error
      || (response.status === 401 ? "Sua sessão expirou. Entre novamente." : "")
      || (rawBody && !responseBody ? rawBody : "")
      || `Não foi possível concluir a ação (erro ${response.status}).`;
    const error = new Error(responseMessage);
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

function setQueueInterface(status, message = "") {
  matchmakingStatus = status;
  const queued = status === "QUEUED";
  timeOptions.forEach((option) => { option.disabled = queued; });
  quickPlayButton.disabled = isChangingQueue;
  quickPlayButton.innerHTML = queued
    ? '<i data-lucide="x"></i> Cancelar busca'
    : 'Procurar adversário <i data-lucide="arrow-right"></i>';
  quickPlayButton.classList.toggle("button-primary", !queued);
  quickPlayButton.classList.toggle("button-secondary", queued);
  matchmakingMessage.classList.remove("error");
  matchmakingMessage.textContent = message || (queued ? "Procurando adversário..." : "");
  refreshIcons();
}

function openMatchedRoom(match) {
  if (!match?.roomCode || isNavigatingToMatch) return;
  isNavigatingToMatch = true;
  setQueueInterface("MATCH_FOUND", "Adversário encontrado!");
  window.setTimeout(() => {
    window.location.href = `room/index.html?mode=public&code=${encodeURIComponent(match.roomCode)}&time=${match.timeControlMinutes}`;
  }, 450);
}

function applyMatchmakingStatus(response) {
  if (response?.status === "MATCH_FOUND") {
    openMatchedRoom(response.match || response);
  } else if (response?.status === "QUEUED") {
    selectedTime = response.timeControlMinutes;
    timeOptions.forEach((option) => option.classList.toggle("active", Number(option.dataset.time) === selectedTime));
    setQueueInterface("QUEUED");
  } else {
    setQueueInterface("IDLE");
  }
}

async function syncMatchmakingStatus() {
  if (!isAuthenticated()) return;
  try {
    applyMatchmakingStatus(await apiRequest("/matchmaking/checkers/queue/status"));
  } catch (error) {
    matchmakingMessage.classList.add("error");
    matchmakingMessage.textContent = error.message;
  }
}

function connectMatchmakingRealtime() {
  if (!isAuthenticated() || !window.StompJs || matchmakingClient?.active) return;
  matchmakingClient = new window.StompJs.Client({
    brokerURL: WS_BASE_URL,
    connectHeaders: { Authorization: `Bearer ${getToken()}` },
    reconnectDelay: 3000,
    debug: () => {},
    onConnect: () => {
      matchmakingSubscription?.unsubscribe();
      matchmakingSubscription = matchmakingClient.subscribe("/user/queue/matchmaking", (message) => {
        try {
          openMatchedRoom(JSON.parse(message.body));
        } catch {
          matchmakingMessage.classList.add("error");
          matchmakingMessage.textContent = "Não foi possível abrir a partida encontrada.";
        }
      });
      syncMatchmakingStatus();
    },
    onWebSocketClose: () => {
      matchmakingSubscription = null;
      if (matchmakingStatus === "QUEUED") {
        matchmakingMessage.textContent = "Reconectando à busca...";
      }
    },
  });
  matchmakingClient.activate();
}

quickPlayButton?.addEventListener("click", async () => {
  const destination = `checkers/index.html?action=quick-play&time=${selectedTime}`;

  if (!isAuthenticated()) {
    requestAuthentication(destination);
    return;
  }

  if (isChangingQueue) return;
  isChangingQueue = true;
  quickPlayButton.disabled = true;
  try {
    const response = matchmakingStatus === "QUEUED"
      ? await apiRequest("/matchmaking/checkers/queue", { method: "DELETE" })
      : await apiRequest("/matchmaking/checkers/queue", {
        method: "POST",
        body: JSON.stringify({ timeControlMinutes: selectedTime }),
      });
    applyMatchmakingStatus(response);
  } catch (error) {
    matchmakingMessage.classList.add("error");
    matchmakingMessage.textContent = error.message;
  } finally {
    isChangingQueue = false;
    quickPlayButton.disabled = false;
  }
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
const requestedTime = Number(new URLSearchParams(window.location.search).get("time"));

if (pageAction === "create-private" && isAuthenticated()) {
  window.history.replaceState({}, "", "index.html");
  window.setTimeout(() => createPrivateRoomButton?.click(), 150);
}

if (pageAction === "quick-play" && isAuthenticated()) {
  if ([5, 10].includes(requestedTime)) {
    selectedTime = requestedTime;
    timeOptions.forEach((option) => option.classList.toggle("active", Number(option.dataset.time) === selectedTime));
  }
  window.history.replaceState({}, "", "index.html");
  window.setTimeout(() => quickPlayButton?.click(), 250);
}

refreshIcons();
connectMatchmakingRealtime();
