const USER_STORAGE_KEY = "duelo64.user";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const { API_BASE_URL, WS_BASE_URL } = window.Duelo64Config;
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
const opponentPresence = document.querySelector("#opponent-presence");
const opponentPhoto = document.querySelector("#opponent-photo");
const playerMeta = document.querySelector("#player-meta");
const chatForm = document.querySelector("#chat-form");
const chatInput = document.querySelector("#chat-input");
const chatMessages = document.querySelector("#chat-messages");
const matchState = document.querySelector("#match-state");
const resultOverlay = document.querySelector("#result-overlay");
const resultTitle = document.querySelector("#result-title");
const resultReason = document.querySelector("#result-reason");
const rematchRequestButton = document.querySelector("#rematch-request-button");
const rematchResponse = document.querySelector("#rematch-response");
const rematchAcceptButton = document.querySelector("#rematch-accept-button");
const rematchDeclineButton = document.querySelector("#rematch-decline-button");
const rematchFeedbackElement = document.querySelector("#rematch-feedback");
const drawOfferButton = document.querySelector("#draw-offer-button");
const drawOfferStatus = document.querySelector("#draw-offer-status");
const resignButton = document.querySelector("#resign-button");
const drawOfferPanel = document.querySelector("#draw-offer-panel");
const drawAcceptButton = document.querySelector("#draw-accept-button");
const drawDeclineButton = document.querySelector("#draw-decline-button");
const resignConfirmationOverlay = document.querySelector("#resign-confirmation-overlay");
const resignCancelButton = document.querySelector("#resign-cancel-button");
const resignConfirmButton = document.querySelector("#resign-confirm-button");
const resignConfirmationFeedback = document.querySelector("#resign-confirmation-feedback");

const board = Array.from({ length: 8 }, () => Array(8).fill(null));
let selectedCell = null;
let dragState = null;
let currentTurn = "WHITE";
let roomStatus = "WAITING";
let playerColor = "WHITE";
let isSubmittingMove = false;
let stompClient = null;
let roomSubscription = null;
let chatSubscription = null;
let chatErrorSubscription = null;
let opponentUserId = null;
let presenceInitialized = false;
let connectedUserIds = new Set();
let matchDurationSeconds = matchTime * 60;
let whiteRemainingSeconds = matchDurationSeconds;
let blackRemainingSeconds = matchDurationSeconds;
let whiteRemainingAtSyncSeconds = matchDurationSeconds;
let blackRemainingAtSyncSeconds = matchDurationSeconds;
let clockSyncedAtServerMillis = Date.now();
let serverClockOffsetMillis = 0;
let timerInterval = null;
let gameFinished = false;
let timeoutRefreshRequested = false;
let forcedCaptureRow = null;
let forcedCaptureColumn = null;
let winnerColor = null;
let finishReason = null;
let drawOfferPending = false;
let drawOfferedByColor = null;
let isSubmittingMatchAction = false;
let legalMovesRequestId = 0;
let latestAppliedMoveCount = -1;
let suppressBoardClickUntil = 0;
let rematchPending = false;
let rematchRequestedByUserId = null;
let isSubmittingRematch = false;
let rematchFeedback = "";
let wasForcedCaptureRequired = false;
let realtimeDisconnected = false;

const DRAG_THRESHOLD_PX = 6;

function notifyError(error, fallback) {
  const message = window.DueloToast?.errorMessage(error, fallback) || fallback;
  window.showToast?.(message, "error");
  return message;
}



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
  opponentUserId = opponent?.id || null;

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

  renderOpponentPresence();
}

function renderOpponentPresence() {
  if (!opponentPresence) return;
  const opponentIsDisconnected = presenceInitialized
    && opponentUserId
    && !gameFinished
    && !connectedUserIds.has(opponentUserId);
  opponentPresence.hidden = !opponentIsDisconnected;
}

function handlePresenceEvent(event) {
  if (event.type === "PRESENCE_SNAPSHOT") {
    connectedUserIds = new Set(event.connectedUserIds || []);
    presenceInitialized = true;
  } else if (event.type === "PLAYER_CONNECTED" && event.userId) {
    connectedUserIds.add(event.userId);
    presenceInitialized = true;
  } else if (event.type === "PLAYER_DISCONNECTED" && event.userId) {
    connectedUserIds.delete(event.userId);
    presenceInitialized = true;
  }

  renderOpponentPresence();
}

function configureRoom() {
  matchModeElement.hidden = true;

  setClockDuration(matchTime);
  invitationPanel.hidden = true;
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
  const incomingMoveCount = Number(state.moveCount);

  if (Number.isFinite(incomingMoveCount) && incomingMoveCount < latestAppliedMoveCount) {
    return false;
  }

  if (Number.isFinite(incomingMoveCount)) {
    latestAppliedMoveCount = incomingMoveCount;
  }

  state.board.forEach((rowCells, row) => {
    rowCells.forEach((piece, column) => {
      board[row][column] = normalizePiece(piece);
    });
  });

  currentTurn = state.currentTurn;
  whiteRemainingSeconds = Number(state.whiteRemainingMillis) / 1000;
  blackRemainingSeconds = Number(state.blackRemainingMillis) / 1000;
  whiteRemainingAtSyncSeconds = whiteRemainingSeconds;
  blackRemainingAtSyncSeconds = blackRemainingSeconds;
  const receivedServerTime = Date.parse(state.serverTime);
  clockSyncedAtServerMillis = Number.isFinite(receivedServerTime) ? receivedServerTime : Date.now();
  serverClockOffsetMillis = clockSyncedAtServerMillis - Date.now();
  forcedCaptureRow = state.forcedCaptureRow;
  forcedCaptureColumn = state.forcedCaptureColumn;
  winnerColor = state.winnerColor;
  finishReason = state.finishReason;
  drawOfferPending = Boolean(state.drawOfferPending);
  drawOfferedByColor = state.drawOfferedByColor;
  gameFinished = state.status === "FINISHED";
  timeoutRefreshRequested = false;
  renderOpponentPresence();

  if (gameFinished) {
    roomStatus = "FINISHED";
  }

  updateTurnLabel();
  selectedCell = null;
  clearHighlights();
  removeDragState();
  renderBoard();
  renderDrawOffer();

  if (gameFinished) {
    stopClock();
    showResultOverlay();
  } else {
    startClock();
  }

  if (!gameFinished && state.mustContinueCapture && forcedCaptureRow != null && forcedCaptureColumn != null) {
    selectedCell = { row: forcedCaptureRow, column: forcedCaptureColumn };
    showMoves(forcedCaptureRow, forcedCaptureColumn);
    matchState.textContent = "Continue capturando";
    if (!wasForcedCaptureRequired) {
      window.showToast?.("Você deve continuar a captura com esta peça.", "warning");
    }
  }
  wasForcedCaptureRequired = Boolean(state.mustContinueCapture);

  return true;
}

function updateInvitationPanel(room) {
  if (!invitationPanel) return;

  const shouldShow = room.roomType === "PRIVATE"
    && room.status === "WAITING"
    && !room.guest;

  invitationPanel.hidden = !shouldShow;
}

function showResultOverlay() {
  if (!resultOverlay || !resultTitle || !resultReason || !gameFinished) return;

  const isDraw = finishReason?.startsWith("DRAW_");
  const playerWon = winnerColor === playerColor;
  const reasonMessages = {
    NO_PIECES: playerWon
      ? "O adversário ficou sem peças."
      : "Você ficou sem peças.",
    NO_LEGAL_MOVES: playerWon
      ? "O adversário ficou sem movimentos disponíveis."
      : "Você ficou sem movimentos disponíveis.",
    TIMEOUT: playerWon
      ? "O tempo do adversário acabou."
      : "Seu tempo acabou.",
    RESIGNATION: playerWon
      ? "O adversário abandonou a partida."
      : "Você abandonou a partida.",
    DRAW_AGREEMENT: "A partida terminou por acordo.",
    DRAW_REPETITION: "A posição se repetiu.",
    DRAW_MOVE_LIMIT: "A partida atingiu o limite de movimentos sem progresso.",
  };

  resultTitle.textContent = isDraw ? "Empate" : playerWon ? "Você venceu!" : "Você perdeu";
  resultReason.textContent = reasonMessages[finishReason] || "A partida foi encerrada.";
  renderRematch();
  resultOverlay.hidden = false;
  window.lucide?.createIcons();
}

function syncRematchFromRoom(room) {
  rematchPending = Boolean(room.rematchPending);
  rematchRequestedByUserId = room.rematchRequestedByUserId || null;
  if (room.rematchRoomCode) {
    navigateToRematch(room.rematchRoomCode);
    return;
  }
  renderRematch();
}

function renderRematch() {
  if (!rematchRequestButton || !rematchResponse) return;
  const currentUserId = readUser()?.id;
  const requestIsMine = rematchPending && rematchRequestedByUserId === currentUserId;
  const requestIsFromOpponent = rematchPending && rematchRequestedByUserId !== currentUserId;

  rematchRequestButton.hidden = requestIsFromOpponent;
  rematchRequestButton.disabled = isSubmittingRematch || requestIsMine || !gameFinished;
  rematchRequestButton.textContent = requestIsMine ? "Aguardando adversário..." : "Pedir revanche";
  rematchResponse.hidden = !requestIsFromOpponent;
  if (rematchAcceptButton) rematchAcceptButton.disabled = isSubmittingRematch;
  if (rematchDeclineButton) rematchDeclineButton.disabled = isSubmittingRematch;
  if (rematchFeedbackElement) rematchFeedbackElement.textContent = rematchFeedback;
}

async function submitRematchAction(action) {
  if (isSubmittingRematch || !gameFinished) return;
  isSubmittingRematch = true;
  rematchFeedback = "";
  renderRematch();

  try {
    const response = await apiRequest(
      `/checkers/rooms/${encodeURIComponent(roomCode)}/rematch/${action}`,
      { method: "POST" },
    );
    if (action === "accept" && response.roomCode) {
      navigateToRematch(response.roomCode);
      return;
    }
    syncRematchFromRoom(response);
  } catch (error) {
    rematchFeedback = error.message;
    notifyError(error, "Não foi possível concluir a ação de revanche.");
  } finally {
    isSubmittingRematch = false;
    renderRematch();
  }
}

function handleRematchEvent(event) {
  if (event?.type === "REMATCH_ACCEPTED" && event.newRoomCode) {
    navigateToRematch(event.newRoomCode);
    return;
  }
  if (event?.type === "REMATCH_DECLINED") {
    const requestWasMine = rematchRequestedByUserId === readUser()?.id;
    rematchPending = false;
    rematchRequestedByUserId = null;
    if (requestWasMine) {
      rematchFeedback = "O adversário recusou a revanche.";
      window.showToast?.("Revanche recusada.", "info");
    }
    renderRematch();
  }
}

function navigateToRematch(newRoomCode) {
  const target = new URL(window.location.href);
  target.searchParams.set("mode", "private");
  target.searchParams.set("code", newRoomCode);
  target.searchParams.set("time", String(matchTime));
  window.location.replace(target.toString());
}

function renderDrawOffer() {
  const offerIsMine = drawOfferPending && drawOfferedByColor === playerColor;
  const offerIsFromOpponent = drawOfferPending && drawOfferedByColor !== playerColor;

  if (drawOfferPanel) drawOfferPanel.hidden = !offerIsFromOpponent || gameFinished;
  if (drawOfferButton) drawOfferButton.disabled = gameFinished || roomStatus !== "IN_PROGRESS" || drawOfferPending || isSubmittingMatchAction;
  if (resignButton) resignButton.disabled = gameFinished || roomStatus !== "IN_PROGRESS" || isSubmittingMatchAction;
  if (drawAcceptButton) drawAcceptButton.disabled = isSubmittingMatchAction;
  if (drawDeclineButton) drawDeclineButton.disabled = isSubmittingMatchAction;
  if (drawOfferStatus) {
    drawOfferStatus.textContent = offerIsMine
      ? "Aguardando resposta do rival"
      : drawOfferPending
        ? "Oferta recebida"
        : "Enviar proposta ao rival";
  }
}

async function submitMatchAction(path) {
  if (isSubmittingMatchAction || gameFinished || roomStatus !== "IN_PROGRESS") return false;

  isSubmittingMatchAction = true;
  renderDrawOffer();
  try {
    const state = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}/state/${path}`, {
      method: "POST",
    });
    syncBoardFromState(state);
    if (path === "draw-offer") window.showToast?.("Oferta de empate enviada.", "info");
    return true;
  } catch (error) {
    notifyError(error, "Não foi possível concluir a ação.");
    updateTurnLabel();
    return false;
  } finally {
    isSubmittingMatchAction = false;
    renderDrawOffer();
  }
}

function openResignConfirmation() {
  if (!resignConfirmationOverlay || gameFinished || roomStatus !== "IN_PROGRESS") return;
  resignConfirmationFeedback.hidden = true;
  resignConfirmationFeedback.textContent = "";
  resignConfirmationOverlay.hidden = false;
  resignCancelButton?.focus();
}

function closeResignConfirmation() {
  if (!resignConfirmationOverlay || isSubmittingMatchAction) return;
  resignConfirmationOverlay.hidden = true;
  resignButton?.focus();
}

async function confirmResignation() {
  if (isSubmittingMatchAction) return;

  resignConfirmButton.disabled = true;
  resignCancelButton.disabled = true;
  resignConfirmationFeedback.hidden = true;

  const succeeded = await submitMatchAction("resign");

  resignConfirmButton.disabled = false;
  resignCancelButton.disabled = false;
  if (succeeded) {
    resignConfirmationOverlay.hidden = true;
    return;
  }

  resignConfirmationFeedback.textContent = "Não foi possível abandonar a partida. Tente novamente.";
  resignConfirmationFeedback.hidden = false;
}


function setClockDuration(minutes) {
  matchDurationSeconds = Math.max(1, Number(minutes) || matchTime) * 60;
  whiteRemainingSeconds = matchDurationSeconds;
  blackRemainingSeconds = matchDurationSeconds;
  whiteRemainingAtSyncSeconds = matchDurationSeconds;
  blackRemainingAtSyncSeconds = matchDurationSeconds;
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

  const estimatedServerNow = Date.now() + serverClockOffsetMillis;
  const elapsedSeconds = Math.max(0, (estimatedServerNow - clockSyncedAtServerMillis) / 1000);

  whiteRemainingSeconds = whiteRemainingAtSyncSeconds;
  blackRemainingSeconds = blackRemainingAtSyncSeconds;

  if (currentTurn === "WHITE") {
    whiteRemainingSeconds = Math.max(0, whiteRemainingAtSyncSeconds - elapsedSeconds);
  } else {
    blackRemainingSeconds = Math.max(0, blackRemainingAtSyncSeconds - elapsedSeconds);
  }

  renderClocks();

  if (whiteRemainingSeconds <= 0 || blackRemainingSeconds <= 0) {
    stopClock();
    requestTimeoutConfirmation();
  }
}

async function requestTimeoutConfirmation() {
  if (timeoutRefreshRequested || gameFinished || roomStatus !== "IN_PROGRESS") return;

  timeoutRefreshRequested = true;
  try {
    const state = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}/state/timeout`, {
      method: "POST",
    });
    syncBoardFromState(state);
  } catch (error) {
    timeoutRefreshRequested = false;
    updateTurnLabel(error.message);
    window.setTimeout(loadGameState, 1000);
  }
}

function startClock() {
  renderClocks();

  if (roomStatus !== "IN_PROGRESS") {
    stopClock();
    return;
  }

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
    if (finishReason?.startsWith("DRAW_")) {
      matchState.textContent = finishReason === "DRAW_AGREEMENT" ? "Empate por acordo" : "Empate";
      playerClock.classList.remove("active");
      opponentClock.classList.remove("active");
      return;
    }
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
    notifyError(error, "Não foi possível carregar o estado da partida.");
    updateTurnLabel();
  }
}

function applyRoom(room) {
  const safeCode = room.code || roomCode;
  roomStatus = room.status;
  setClockDuration(room.timeControlMinutes);
  updatePlayersFromRoom(room);
  syncRematchFromRoom(room);
  updateInvitationPanel(room);
  matchModeElement.hidden = true;
  matchModeElement.textContent = "";
  roomCodeHeader.hidden = false;
  roomCodeHeader.textContent = `SALA ${safeCode}`;
  roomInviteCode.textContent = safeCode;
}

async function loadRoom() {
  if (!roomCode) {
    matchModeElement.textContent = "Sala nao encontrada";
    return false;
  }

  try {
    const path = mode === "private"
      ? `/checkers/rooms/${encodeURIComponent(roomCode)}/join`
      : `/checkers/rooms/${encodeURIComponent(roomCode)}`;
    const room = await apiRequest(path, mode === "private" ? { method: "POST" } : {});

    applyRoom(room);
    await loadGameState();
    return true;
  } catch (error) {
    if (error.status === 401 || error.status === 403) {
      sessionStorage.removeItem(TOKEN_STORAGE_KEY);
      requireAuthentication();
      return false;
    }

    matchModeElement.hidden = false;
    matchModeElement.textContent = error.message;
    notifyError(error, "Não foi possível carregar a sala.");
    invitationPanel.hidden = true;
    roomCodeHeader.hidden = true;
    return false;
  }
}

async function refreshRoom() {
  if (!roomCode) return;

  try {
    const room = await apiRequest(`/checkers/rooms/${encodeURIComponent(roomCode)}`);
    applyRoom(room);
    await loadGameState();
  } catch (error) {
    notifyError(error, "Não foi possível atualizar a sala.");
    updateTurnLabel();
  }
}

function connectRoomRealtime() {
  if (!roomCode || !window.StompJs) return;

  stompClient = new window.StompJs.Client({
    brokerURL: WS_BASE_URL,
    connectHeaders: {
      Authorization: `Bearer ${getToken()}`,
    },
    reconnectDelay: 3000,
    debug: () => { },
    onConnect: () => {
      if (realtimeDisconnected) window.showToast?.("Conexão restabelecida.", "success");
      realtimeDisconnected = false;
      try {
        roomSubscription?.unsubscribe();
        chatSubscription?.unsubscribe();
        chatErrorSubscription?.unsubscribe();
      } catch {
        // A assinatura da conexão anterior já foi encerrada pelo servidor.
      }

      roomSubscription = stompClient.subscribe(`/topic/rooms/${roomCode}`, (message) => {
        try {
          const event = JSON.parse(message.body);
          handlePresenceEvent(event);
          handleRematchEvent(event);
        } catch {
          // Eventos sem corpo valido ainda provocam a ressincronizacao da sala.
        }
        window.setTimeout(refreshRoom, 80);
      });
      chatSubscription = stompClient.subscribe(`/topic/rooms/${roomCode}/chat`, (message) => {
        try {
          renderChatMessage(JSON.parse(message.body));
        } catch {
          // Uma mensagem malformada nunca deve quebrar a partida.
        }
      });
      chatErrorSubscription = stompClient.subscribe("/user/queue/chat-errors", (message) => {
        try {
          renderChatFeedback(JSON.parse(message.body)?.message);
        } catch {
          renderChatFeedback("Nao foi possivel enviar a mensagem.");
        }
      });
      refreshRoom();
    },
    onStompError: () => {
      updateTurnLabel("Tempo real indisponivel");
      window.showToast?.("Tempo real indisponível.", "warning");
    },
    onWebSocketError: () => {
      updateTurnLabel("Reconectando...");
      if (!realtimeDisconnected) window.showToast?.("Reconectando...", "warning");
      realtimeDisconnected = true;
    },
    onWebSocketClose: () => {
      roomSubscription = null;
      chatSubscription = null;
      chatErrorSubscription = null;
      if (!gameFinished) updateTurnLabel("Reconectando...");
      if (!gameFinished && !realtimeDisconnected) window.showToast?.("Reconectando...", "warning");
      realtimeDisconnected = !gameFinished;
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
  legalMovesRequestId += 1;
  document.querySelectorAll(".board-cell").forEach((cell) => cell.classList.remove("selected", "valid-move"));
}

function renderChatFeedback(message) {
  if (!message) return;
  notifyError({ message }, "Não foi possível enviar a mensagem.");
}

function renderChatMessage(message) {
  if (!message?.userId || typeof message.text !== "string") return;

  const currentUser = readUser();
  const isOwnMessage = currentUser?.id === message.userId;
  const shouldAutoScroll = isOwnMessage || isChatNearBottom();
  const article = document.createElement("article");
  article.className = "chat-message";
  if (isOwnMessage) article.classList.add("own-message");

  const author = document.createElement("span");
  author.className = "chat-message-author";
  const nickname = String(message.nickname || "Jogador").replace(/^@/, "");
  author.textContent = `@${nickname}`;

  const text = document.createElement("span");
  text.className = "chat-message-text";
  text.textContent = message.text;

  const time = document.createElement("time");
  time.className = "chat-message-time";
  const timestamp = new Date(message.timestamp);
  time.textContent = Number.isNaN(timestamp.getTime())
    ? ""
    : timestamp.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });

  article.append(author, text, time);
  chatMessages.appendChild(article);
  if (shouldAutoScroll) scrollChatToBottom();
}

function isChatNearBottom() {
  const distanceFromBottom = chatMessages.scrollHeight - chatMessages.scrollTop - chatMessages.clientHeight;
  return distanceFromBottom <= 40;
}

function scrollChatToBottom() {
  chatMessages.scrollTo({
    top: chatMessages.scrollHeight,
    behavior: "smooth",
  });
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
    origin: { row, column },
    startX: event.clientX,
    startY: event.clientY,
    didMove: false,
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

  const distance = Math.hypot(
    event.clientX - dragState.startX,
    event.clientY - dragState.startY,
  );

  if (distance >= DRAG_THRESHOLD_PX) {
    dragState.didMove = true;
  }

  updateDragPiece(event.clientX, event.clientY);
}

function finishPieceDrag(event) {
  if (!dragState) return;

  event.preventDefault();
  const target = getCellFromPoint(event.clientX, event.clientY);
  const { didMove, origin } = dragState;
  const changedCell = target
    && (target.row !== origin.row || target.column !== origin.column);

  if (didMove && changedCell) {
    suppressBoardClickUntil = performance.now() + 250;
    movePiece(target.row, target.column);
  }

  removeDragState();
}

async function showMoves(row, column) {
  clearHighlights();
  const requestId = legalMovesRequestId;
  const selected = boardElement.querySelector(`[data-row="${row}"][data-column="${column}"]`);
  selected?.classList.add("selected");

  try {
    const response = await apiRequest(
      `/checkers/rooms/${encodeURIComponent(roomCode)}/state/legal-moves?row=${row}&column=${column}`,
    );

    const selectionChanged = !selectedCell
      || selectedCell.row !== row
      || selectedCell.column !== column;

    if (requestId !== legalMovesRequestId || selectionChanged) return;

    response.moves.forEach((move) => {
      boardElement
        .querySelector(`[data-row="${move.toRow}"][data-column="${move.toColumn}"]`)
        ?.classList.add("valid-move");
    });
  } catch {
    // O POST continua sendo a autoridade; falha no auxilio nao bloqueia a partida.
  }
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
    notifyError(error, "Movimento inválido.");
    updateTurnLabel();
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
  if (!selectedCell || isSubmittingMove) return;

  const from = selectedCell;
  selectedCell = null;
  submitMove(from, { row: targetRow, column: targetColumn });
}

function handleCellClick(row, column) {
  if (performance.now() < suppressBoardClickUntil || isSubmittingMove) {
    return;
  }

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
  try {
    await navigator.clipboard.writeText(roomCode);
    copyInviteButton.innerHTML = '<i data-lucide="check"></i>';
    window.lucide?.createIcons();
    window.showToast?.("Convite copiado.", "success");
  } catch (error) {
    notifyError(error, "Não foi possível copiar o convite.");
  }
});

drawOfferButton?.addEventListener("click", () => submitMatchAction("draw-offer"));
drawAcceptButton?.addEventListener("click", () => submitMatchAction("draw-accept"));
drawDeclineButton?.addEventListener("click", () => submitMatchAction("draw-decline"));
resignButton?.addEventListener("click", openResignConfirmation);
resignCancelButton?.addEventListener("click", closeResignConfirmation);
resignConfirmButton?.addEventListener("click", confirmResignation);
rematchRequestButton?.addEventListener("click", () => submitRematchAction("request"));
rematchAcceptButton?.addEventListener("click", () => submitRematchAction("accept"));
rematchDeclineButton?.addEventListener("click", () => submitRematchAction("decline"));
resignConfirmationOverlay?.addEventListener("click", (event) => {
  if (event.target === resignConfirmationOverlay) closeResignConfirmation();
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !resignConfirmationOverlay?.hidden) {
    closeResignConfirmation();
  }
});

chatForm?.addEventListener("submit", (event) => {
  event.preventDefault();
  const message = chatInput.value.trim();
  if (!message || !stompClient?.connected) return;

  stompClient.publish({
    destination: `/app/rooms/${roomCode}/chat`,
    body: JSON.stringify({ text: message }),
  });
  chatInput.value = "";
});

async function initializeRoomPage() {
  setupBoard();
  configurePlayer();
  configureRoom();
  renderBoard();
  window.lucide?.createIcons();

  const loaded = await loadRoom();
  if (loaded) connectRoomRealtime();
}

if (requireAuthentication()) {
  initializeRoomPage();
}
