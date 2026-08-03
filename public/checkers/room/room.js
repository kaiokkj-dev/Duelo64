const USER_STORAGE_KEY = "duelo64.user";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
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
const chatForm = document.querySelector("#chat-form");
const chatInput = document.querySelector("#chat-input");
const chatMessages = document.querySelector("#chat-messages");

const board = Array.from({ length: 8 }, () => Array(8).fill(null));
let selectedCell = null;

function requireAuthentication() {
  if (sessionStorage.getItem(TOKEN_STORAGE_KEY)) return true;

  const destination = `checkers/room/index.html${window.location.search}`;
  window.location.replace(`../../entrar.html?returnTo=${encodeURIComponent(destination)}`);
  return false;
}

window.addEventListener("duelo64:session-expired", requireAuthentication);

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
  const playerName = document.querySelector("#player-name");
  const playerPhoto = document.querySelector("#player-photo");
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

function configureRoom() {
  const formattedTime = `${String(matchTime).padStart(2, "0")}:00`;
  playerClock.textContent = formattedTime;
  opponentClock.textContent = formattedTime;

  if (mode !== "private") return;

  const safeCode = roomCode || "AGUARDANDO";
  matchModeElement.textContent = "Sala privada";
  invitationPanel.hidden = false;
  roomCodeHeader.hidden = false;
  roomCodeHeader.textContent = `SALA ${safeCode}`;
  roomInviteCode.textContent = safeCode;
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

function showMoves(row, column) {
  clearHighlights();
  const selected = boardElement.querySelector(`[data-row="${row}"][data-column="${column}"]`);
  selected?.classList.add("selected");

  const direction = board[row][column] === "white" ? -1 : 1;
  [[direction, -1], [direction, 1]].forEach(([rowStep, columnStep]) => {
    const targetRow = row + rowStep;
    const targetColumn = column + columnStep;

    if (targetRow < 0 || targetRow > 7 || targetColumn < 0 || targetColumn > 7) return;
    if (board[targetRow][targetColumn]) return;

    boardElement.querySelector(`[data-row="${targetRow}"][data-column="${targetColumn}"]`)?.classList.add("valid-move");
  });
}

function movePiece(targetRow, targetColumn) {
  if (!selectedCell) return;
  const target = boardElement.querySelector(`[data-row="${targetRow}"][data-column="${targetColumn}"]`);
  if (!target?.classList.contains("valid-move")) return;

  board[targetRow][targetColumn] = board[selectedCell.row][selectedCell.column];
  board[selectedCell.row][selectedCell.column] = null;
  selectedCell = null;
  renderBoard();
}

function handleCellClick(row, column) {
  if (board[row][column] === "white") {
    selectedCell = { row, column };
    showMoves(row, column);
    return;
  }

  movePiece(row, column);
}

function renderBoard() {
  boardElement.innerHTML = "";

  for (let row = 0; row < 8; row += 1) {
    for (let column = 0; column < 8; column += 1) {
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
        cell.appendChild(piece);
      }

      cell.addEventListener("click", () => handleCellClick(row, column));
      boardElement.appendChild(cell);
    }
  }
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
  window.lucide?.createIcons();
}
