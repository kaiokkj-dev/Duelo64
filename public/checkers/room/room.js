const USER_STORAGE_KEY = "duelo64.user";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const API_BASE_URL = "http://localhost:8080/api/v1";
const WS_BASE_URL = "ws://localhost:8080/ws";
const params = new URLSearchParams(window.location.search);
const mode = params.get("mode") || "public";
const roomCode = params.get("code") || "";
const matchTime = Number(params.get("time")) || 10;

const boardElement = document.querySelector("#room-board");
const matchModeElement = document.querySelector("#match-mode");
const invitationPanel = document.querySelector("#invitation-panel");
const roomCodeHeader = document.querySelector("#room-code-header");
const roomInviteCode = document.querySelector("#room-invite-code");
const copyInviteButton = document.querySelector("#copy-invite");
const playerClock = document.querySelector("#player-clock");
const opponentClock = document.querySelector("#opponent-clock");
const playerName = document.querySelector("#player-name");
const playerPhoto = document.querySelector("#player-photo");
const opponentName = document.querySelector("#opponent-name");
const opponentMeta = document.querySelector("#opponent-meta");
const opponentPhoto = document.querySelector("#opponent-photo");
const playerMeta = document.querySelector("#player-meta");
const chatForm = document.querySelector("#chat-form");
const chatInput = document.querySelector("#chat-input");
const chatMessages = document.querySelector("#chat-messages");
const matchState = document.querySelector("#match-state");

const board = Array.from({ length: 8 }, () => Array(8).fill(null));
let selectedCell = null;
let dragState = null;
let currentTurn = "WHITE";
let roomStatus = "WAITING";
let playerColor = "WHITE";
let isSubmittingMove = false;
let stompClient = null;
let matchDurationSeconds = matchTime * 60;
let whiteRemainingSeconds = matchDurationSeconds;
let blackRemainingSeconds = matchDurationSeconds;
let timerInterval = null;
let lastTimerTick = Date.now();
let gameFinished = false;
let timeoutRefreshRequested = false;
let forcedCaptureRow = null;
let forcedCaptureColumn = null;
let winnerColor = null;
let finishReason = null;



function requireAuthentication() {
  if (sessionStorage.getItem(TOKEN_STORAGE_KEY)) return true;

  const destination = `checkers/room/index.html${window.location.search}`;
  window.location.replace(`../../entrar.html?returnTo=${encodeURIComponent(destination)}`);
  return false;
}

window.addEventListener("duelo64:session-expired", requireAuthentication);

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
    const error = new Error(responseBody?.message || "Nao foi possivel carregar a sala.");
    error.code = responseBody?.code;
    error.status = response.status;
    throw error;
  }

  return responseBody;
}

function readUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_STORAGE_KEY));
  } catch {
    return null;
  }
}

function configurePlayer() {
  const user = readUser();
  if (!user) return;

  const nickname = user.nickname?.replace(/^@/, "") || "jogador";
  playerName.textContent = `@${nickname}`;

  if (user.avatarUrl) {
    const image = document.createElement("img");
    image.src = user.avatarUrl;
    image.alt = `Avatar de @${nickname}`;
    playerPhoto.replaceChildren(image);
  } else {
    playerPhoto.textContent = nickname.charAt(0).toUpperCase();
  }
}

function nicknameLabel(user, fallback = "jogador") {
  const nickname = user?.nickname?.replace(/^@/, "") || fallback;
  return `@${nickname}`;
}

function renderPlayerAvatar(element, user, fallbackInitial) {
  if (!element) return;

  if (user?.avatarUrl) {
    const image = document.createElement("img");
    image.src = user.avatarUrl;
    image.alt = `Avatar de ${nicknameLabel(user)}`;
    element.replaceChildren(image);
    return;
  }

  element.textContent = fallbackInitial;
}

function updatePlayersFromRoom(room) {
  const currentUser = readUser();
  const isHost = currentUser?.id && room.host?.id === currentUser.id;
  const you = isHost ? room.host : room.guest;
  const opponent = isHost ? room.guest : room.host;

  playerColor = isHost ? "WHITE" : "BLACK";
  renderBoardCoordinates();

  if (you) {
    playerName.textContent = nicknameLabel(you);
    playerMeta.textContent = playerColor === "WHITE" ? "Brancas" : "Pretas";
    renderPlayerAvatar(playerPhoto, you, "J");
  }

  if (opponent) {
    opponentName.textContent = nicknameLabel(opponent);
    opponentMeta.textContent = playerColor === "WHITE" ? "Pretas" : "Brancas";
    renderPlayerAvatar(opponentPhoto, opponent, "R");
  } else {
    opponentName.textContent = "Aguardando rival";
    opponentMeta.textContent = "Convide pelo codigo";
    opponentPhoto.textContent = "R";
  }
}

function configureRoom() {
  matchModeElement.hidden = true;

  setClockDuration(matchTime);

  if (mode !== "private") return;

  const safeCode = roomCode || "AGUARDANDO";
  matchModeElement.hidden = true;
  invitationPanel.hidden = false;
  roomCodeHeader.hidden = false;
  roomCodeHeader.textContent = `SALA ${safeCode}`;
  roomInviteCode.textContent = safeCode;
}

function normalizePiece(piece) {
  if (piece === "." || !piece) return null;
  if (piece === "w") return "white";
  if (piece === "W") return "white king";
  if (piece === "b") return "black";
  if (piece === "B") return "black king";
  return null;
}

function syncBoardFromState(state) {
  state.board.forEach((rowCells, row) => {
    rowCells.forEach((piece, column) => {
      board[row][column] = normalizePiece(piece);
    });
  });

  currentTurn = state.currentTurn;
  whiteRemainingSeconds = Number(state.whiteRemainingMillis) / 1000;
  blackRemainingSeconds = Number(state.blackRemainingMillis) / 1000;
  forcedCaptureRow = state.forcedCaptureRow;
  forcedCaptureColumn = state.forcedCaptureColumn;
  winnerColor = state.winnerColor;
  finishReason = state.finishReason;
  gameFinished = state.status === "FINISHED";
  timeoutRefreshRequested = false;

  if (gameFinished) {
    roomStatus = "FINISHED";
  }

  updateTurnLabel();
  startClock();
  selectedCell = null;
  clearHighlights();
  renderBoard();

  if (state.mustContinueCapture && forcedCaptureRow != null && forcedCaptureColumn != null) {
    selectedCell = { row: forcedCaptureRow, column: forcedCaptureColumn };
    showMoves(forcedCaptureRow, forcedCaptureColumn);
    matchState.textContent = "Continue capturando";
  }
}


function setClockDuration(minutes) {
  matchDurationSeconds = Math.max(1, Number(minutes) || matchTime) * 60;
  whiteRemainingSeconds = matchDurationSeconds;
  blackRemainingSeconds = matchDurationSeconds;
  renderClocks();
}

function formatClock(totalSeconds) {
  const safeSeconds = Math.max(0, Math.ceil(totalSeconds));
  const minutes = Math.floor(safeSeconds / 60);
  const seconds = safeSeconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function renderClocks() {
  const playerSeconds = playerColor === "WHITE" ? whiteRemainingSeconds : blackRemainingSeconds;
  const opponentSeconds = playerColor === "WHITE" ? blackRemainingSeconds : whiteRemainingSeconds;

  playerClock.textContent = formatClock(playerSeconds);
  opponentClock.textContent = formatClock(opponentSeconds);
}

function stopClock() {
  if (!timerInterval) return;

  window.clearInterval(timerInterval);
  timerInterval = null;
}

function tickClock() {
  if (roomStatus !== "IN_PROGRESS") {
    stopClock();
    renderClocks();
    return;
  }

  const now = Date.now();
  const elapsedSeconds = (now - lastTimerTick) / 1000;
  lastTimerTick = now;

  if (currentTurn === "WHITE") {
    whiteRemainingSeconds = Math.max(0, whiteRemainingSeconds - elapsedSeconds);
  } else {
    blackRemainingSeconds = Math.max(0, blackRemainingSeconds - elapsedSeconds);
  }

  renderClocks();

  if (whiteRemainingSeconds <= 0 || blackRemainingSeconds <= 0) {
    stopClock();
    gameFinished = true;
    updateTurnLabel(currentTurn === playerColor ? "Tempo esgotado — derrota" : "Tempo esgotado — vitória");

    if (!timeoutRefreshRequested) {
      timeoutRefreshRequested = true;
      loadGameState();
    }
  }
}

function startClock() {
  renderClocks();

  if (roomStatus !== "IN_PROGRESS") {
    stopClock();
    return;
  }

  lastTimerTick = Date.now();

  if (timerInterval) return;

  timerInterval = window.setInterval(tickClock, 1000);
}

function updateTurnLabel(message) {
  if (message) {
    matchState.textContent = message;
    return;
  }

  if (roomStatus === "WAITING") {
    matchState.textContent = "Aguardando rival";
    playerClock.classList.add("active");
    opponentClock.classList.remove("active");
    return;
  }

  if (roomStatus === "FINISHED" || gameFinished) {
    const playerWon = winnerColor === playerColor;
    const reasonLabel = finishReason === "TIMEOUT"
      ? "por tempo"
      : finishReason === "NO_PIECES"
        ? "— rival sem peças"
        : "— rival sem jogadas";
    matchState.textContent = playerWon ? `Você venceu ${reasonLabel}` : `Você perdeu ${reasonLabel}`;
    playerClock.classList.remove("active");
    opponentClock.classList.remove("active");
    return;
  }

  const isYourTurn = currentTurn === playerColor;
  matchState.textContent = isYourTurn ? "Sua vez" : "Vez do rival";
  playerClock.classList.toggle("active", isYourTurn);
  opponentClock.classList.toggle("active", !isYourTurn);
}

async function loadGameState() {
  if (!roomCode) return;

  try {
    const state = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}/state`);
    syncBoardFromState(state);
  } catch (error) {
    updateTurnLabel(error.message);
  }
}

async function loadPrivateRoom() {
  if (mode !== "private") return;

  if (!roomCode) {
    matchModeElement.textContent = "Sala nao encontrada";
    return;
  }

  try {
    const room = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}/join`, {
      method: "POST",
    });

    const safeCode = room.code || roomCode;
    roomStatus = room.status;
    updatePlayersFromRoom(room);
    updateInvitationPanel(room);
    setClockDuration(room.timeControlMinutes);
    matchModeElement.hidden = true;
    roomCodeHeader.hidden = false;
    roomCodeHeader.textContent = `SALA ${safeCode}`;
    roomInviteCode.textContent = safeCode;
    await loadGameState();
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
      requireAuthentication();
      return;
    }

    matchModeElement.hidden = false;
    matchModeElement.textContent = error.message;
    invitationPanel.hidden = true;
    roomCodeHeader.hidden = true;
  }
}

async function refreshPrivateRoom() {
  if (mode !== "private" || !roomCode) return;

  try {
    const room = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}`);
    roomStatus = room.status;
    updatePlayersFromRoom(room);
    matchModeElement.hidden = true;
    matchModeElement.textContent = "";
    matchState.textContent = room.status === "WAITING" ? "Aguardando rival" : "Sua vez";

    const safeCode = room.code || roomCode;
    if (whiteRemainingSeconds === matchDurationSeconds && blackRemainingSeconds === matchDurationSeconds) {
      setClockDuration(room.timeControlMinutes);
    }
    roomCodeHeader.hidden = false;
    roomCodeHeader.textContent = `SALA ${safeCode}`;
    roomInviteCode.textContent = safeCode;

    await loadGameState();
  } catch (error) {
    updateTurnLabel(error.message);
  }
}

function connectRoomRealtime() {
  if (!roomCode || !window.StompJs) return;

  stompClient = new window.StompJs.Client({
    brokerURL: WS_BASE_URL,
    reconnectDelay: 3000,
    debug: () => { },
    onConnect: () => {
      stompClient.subscribe(`/topic/rooms/${roomCode}`, () => {
        window.setTimeout(refreshPrivateRoom, 80);
      });
    },
    onStompError: () => {
      updateTurnLabel("Tempo real indisponivel");
    },
    onWebSocketError: () => {
      updateTurnLabel("Reconectando...");
    },
  });

  stompClient.activate();
}

function setupBoard() {
  for (let row = 0; row < 8; row += 1) {
    for (let column = 0; column < 8; column += 1) {
      if ((row + column) % 2 === 1 && row < 3) board[row][column] = "black";
      if ((row + column) % 2 === 1 && row > 4) board[row][column] = "white";
    }
  }
}

function clearHighlights() {
  document.querySelectorAll(".board-cell").forEach((cell) => cell.classList.remove("selected", "valid-move"));
}

function getCellFromPoint(clientX, clientY) {
  const element = document.elementFromPoint(clientX, clientY);
  const cell = element?.closest?.(".board-cell");

  if (!cell || !boardElement.contains(cell)) {
    return null;
  }

  return {
    row: Number(cell.dataset.row),
    column: Number(cell.dataset.column),
  };
}

function updateDragPiece(clientX, clientY) {
  if (!dragState) return;

  dragState.ghost.style.transform = `translate(${clientX - dragState.offsetX}px, ${clientY - dragState.offsetY}px)`;
}

function removeDragState() {
  if (!dragState) return;

  dragState.sourcePiece.classList.remove("is-drag-source");
  dragState.ghost.remove();
  dragState = null;
}

function startPieceDrag(event, row, column, piece) {
  if (gameFinished || roomStatus === "FINISHED") {
    updateTurnLabel();
    return;
  }
  if (roomStatus !== "IN_PROGRESS") {
    updateTurnLabel("Aguardando rival");
    return;
  }

  if (!canSelectPiece(row, column)) return;

  event.preventDefault();
  selectedCell = { row, column };
  showMoves(row, column);

  const pieceRect = piece.getBoundingClientRect();
  const ghost = piece.cloneNode(true);
  ghost.classList.add("checker-drag-ghost");
  ghost.style.width = `${pieceRect.width}px`;
  ghost.style.height = `${pieceRect.height}px`;
  document.body.appendChild(ghost);

  dragState = {
    ghost,
    sourcePiece: piece,
    offsetX: pieceRect.width / 2,
    offsetY: pieceRect.height / 2,
  };

  piece.classList.add("is-drag-source");
  piece.setPointerCapture?.(event.pointerId);
  updateDragPiece(event.clientX, event.clientY);
}

function handlePieceDrag(event) {
  if (!dragState) return;

  event.preventDefault();
  updateDragPiece(event.clientX, event.clientY);
}

function finishPieceDrag(event) {
  if (!dragState) return;

  event.preventDefault();
  const target = getCellFromPoint(event.clientX, event.clientY);

  if (target) {
    movePiece(target.row, target.column);
  }

  removeDragState();
}

function showMoves(row, column) {
  clearHighlights();
  const selected = boardElement.querySelector(`[data-row="${row}"][data-column="${column}"]`);
  selected?.classList.add("selected");
}

async function submitMove(from, to) {
  if (isSubmittingMove) return;

  if (gameFinished || roomStatus === "FINISHED") {
    updateTurnLabel();
    return;
  }

  if (roomStatus !== "IN_PROGRESS") {
    updateTurnLabel("Aguardando rival");
    return;
  }

  isSubmittingMove = true;
  updateTurnLabel("Validando...");

  try {
    const state = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}/state/moves`, {
      method: "POST",
      body: JSON.stringify({
        fromRow: from.row,
        fromColumn: from.column,
        toRow: to.row,
        toColumn: to.column,
      }),
    });

    syncBoardFromState(state);
  } catch (error) {
    updateTurnLabel(error.message);
    clearHighlights();
  } finally {
    isSubmittingMove = false;
  }
}

function isOwnPiece(piece) {
  if (!piece) return false;

  if (playerColor === "WHITE") {
    return piece.startsWith("white");
  }

  return piece.startsWith("black");
}

function canSelectPiece(row, column) {
  if (!isOwnPiece(board[row][column])) return false;

  if (forcedCaptureRow == null || forcedCaptureColumn == null) {
    return true;
  }

  return row === forcedCaptureRow && column === forcedCaptureColumn;
}

function movePiece(targetRow, targetColumn) {
  if (!selectedCell) return;

  const from = selectedCell;
  selectedCell = null;
  submitMove(from, { row: targetRow, column: targetColumn });
}

function handleCellClick(row, column) {
  if (gameFinished || roomStatus === "FINISHED") {
    updateTurnLabel();
    return;
  }
  if (roomStatus !== "IN_PROGRESS") {
    updateTurnLabel("Aguardando rival");
    return;
  }

  if (canSelectPiece(row, column)) {
    selectedCell = { row, column };
    showMoves(row, column);
    return;
  }

  if (isOwnPiece(board[row][column])) {
    updateTurnLabel("Continue a captura com a peça marcada");
    return;
  }

  movePiece(row, column);
}

function renderBoard() {
  boardElement.innerHTML = "";

  for (let displayRow = 0; displayRow < 8; displayRow += 1) {
    for (let displayColumn = 0; displayColumn < 8; displayColumn += 1) {
      const row = playerColor === "BLACK" ? 7 - displayRow : displayRow;
      const column = playerColor === "BLACK" ? 7 - displayColumn : displayColumn;
      const cell = document.createElement("button");
      const isDark = (row + column) % 2 === 1;
      cell.type = "button";
      cell.className = `board-cell ${isDark ? "dark" : "light"}`;
      cell.dataset.row = row;
      cell.dataset.column = column;
      cell.setAttribute("role", "gridcell");
      cell.setAttribute("aria-label", `Casa ${String.fromCharCode(65 + column)}${8 - row}`);

      if (board[row][column]) {
        const piece = document.createElement("span");
        piece.className = `checker ${board[row][column]}`;
        piece.addEventListener("pointerdown", (event) => startPieceDrag(event, row, column, piece));
        piece.addEventListener("pointermove", handlePieceDrag);
        piece.addEventListener("pointerup", finishPieceDrag);
        piece.addEventListener("pointercancel", removeDragState);
        cell.appendChild(piece);
      }

      cell.addEventListener("click", () => handleCellClick(row, column));
      boardElement.appendChild(cell);
    }
  }
}

function renderBoardCoordinates() {
  const labels = playerColor === "BLACK"
    ? ["H", "G", "F", "E", "D", "C", "B", "A"]
    : ["A", "B", "C", "D", "E", "F", "G", "H"];

  document.querySelectorAll(".board-files span").forEach((element, index) => {
    element.textContent = labels[index];
  });
}

copyInviteButton?.addEventListener("click", async () => {
  await navigator.clipboard.writeText(roomCode);
  copyInviteButton.innerHTML = '<i data-lucide="check"></i>';
  window.lucide?.createIcons();
});

chatForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  const message = chatInput.value.trim();
  if (!message) return;

  const paragraph = document.createElement("p");
  paragraph.className = "chat-message";
  paragraph.textContent = message;
  chatMessages.appendChild(paragraph);
  chatInput.value = "";
  chatMessages.scrollTop = chatMessages.scrollHeight;
});

if (requireAuthentication()) {
  setupBoard();
  configurePlayer();
  configureRoom();
  renderBoard();
  loadPrivateRoom();
  connectRoomRealtime();
  window.lucide?.createIcons();
}
function updateInvitationPanel(room) {
  if (!invitationPanel) return;
  const hasGuest = Boolean(room.guest);
  const matchStarted = room.status === "IN_PROGRESS";
  invitationPanel.hidden = hasGuest || matchStarted;
}
