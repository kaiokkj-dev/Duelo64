const { API_BASE_URL } = window.Duelo64Config;
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const USER_STORAGE_KEY = "duelo64.user";

const rankingList = document.querySelector("#ranking-list");
const rankingEmpty = document.querySelector("#ranking-empty");
const previousButton = document.querySelector("#ranking-previous");
const nextButton = document.querySelector("#ranking-next");
const pageLabel = document.querySelector("#ranking-page-label");
const myRanking = document.querySelector("#my-ranking");
const myPosition = document.querySelector("#my-ranking-position");
const myRating = document.querySelector("#my-ranking-rating");
const rankingTable = document.querySelector("#ranking-table");
const rankingTitle = document.querySelector("#ranking-title");
const rankingEyebrow = document.querySelector("#ranking-eyebrow");
const rankingDescription = document.querySelector("#ranking-description");
const gameButtons = [...document.querySelectorAll("[data-ranking-game]")];

const GAME_CONFIG = {
  checkers: {
    label: "Damas Brasileiras",
    heading: "Ranking de Damas",
    title: "Ranking de Damas — Duelo64",
    description: "Ranking ranqueado de Damas do Duelo64.",
  },
  chess: {
    label: "Xadrez",
    heading: "Ranking de Xadrez",
    title: "Ranking de Xadrez — Duelo64",
    description: "Ranking ranqueado de Xadrez do Duelo64.",
  },
};

const initialParams = new URLSearchParams(window.location.search);
let currentGame = GAME_CONFIG[initialParams.get("game")] ? initialParams.get("game") : "checkers";
let currentPage = Math.max(0, Number(initialParams.get("page")) || 0);
let signedInUser = null;
let rankingLoadVersion = 0;
let myRankingLoadVersion = 0;

try {
  signedInUser = JSON.parse(sessionStorage.getItem(USER_STORAGE_KEY));
} catch {
  signedInUser = null;
}

function playerLink(player) {
  const link = document.createElement("a");
  link.className = "ranking-player";
  link.href = `perfil.html?id=${encodeURIComponent(player.userId)}`;

  const avatar = document.createElement("span");
  avatar.className = "ranking-avatar";
  if (player.avatarUrl) {
    const image = document.createElement("img");
    image.src = player.avatarUrl;
    image.alt = "";
    avatar.appendChild(image);
  } else {
    avatar.textContent = (player.nickname || "J").charAt(0).toUpperCase();
  }

  const nickname = document.createElement("span");
  nickname.className = "ranking-nickname";
  nickname.textContent = `@${player.nickname || "jogador"}`;
  link.append(avatar, nickname);
  return link;
}

function renderRanking(page) {
  rankingList.replaceChildren();
  rankingEmpty.querySelector("strong").textContent = "O ranking ainda está vazio.";
  rankingEmpty.querySelector("p").textContent = "As primeiras partidas RANKED aparecerão aqui.";
  rankingEmpty.hidden = page.content.length > 0;
  rankingTable.hidden = page.content.length === 0;

  page.content.forEach((player) => {
    const row = document.createElement("article");
    row.className = "ranking-row";
    row.setAttribute("role", "row");
    if (player.position <= 3) row.classList.add("top-three");
    if (signedInUser?.id === player.userId) row.classList.add("current-user");

    const position = document.createElement("span");
    position.className = "ranking-position";
    position.textContent = `#${player.position}`;

    const rating = document.createElement("strong");
    rating.className = "ranking-rating";
    rating.textContent = player.rating;

    const games = document.createElement("span");
    games.className = "ranking-stat";
    games.textContent = `${player.rankedGames} partidas`;

    const results = document.createElement("span");
    results.className = "ranking-stat";
    results.textContent = `${player.rankedWins}/${player.rankedLosses}/${player.rankedDraws}`;

    const winRate = document.createElement("span");
    winRate.className = "ranking-stat";
    winRate.textContent = `${player.rankedWinRate.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;

    row.append(position, playerLink(player), rating, games, results, winRate);
    rankingList.appendChild(row);
  });

  currentPage = page.page;
  pageLabel.textContent = `Página ${page.page + 1}${page.totalPages ? ` de ${page.totalPages}` : ""}`;
  previousButton.disabled = page.page <= 0;
  nextButton.disabled = page.page + 1 >= page.totalPages;
  updateUrl();
}

async function loadRanking(page = currentPage) {
  const requestedGame = currentGame;
  const loadVersion = ++rankingLoadVersion;
  previousButton.disabled = true;
  nextButton.disabled = true;
  try {
    const response = await fetch(`${API_BASE_URL}/rankings/${requestedGame}?page=${page}`);
    if (!response.ok) throw new Error();
    const result = await response.json();
    if (loadVersion !== rankingLoadVersion || requestedGame !== currentGame) return;
    renderRanking(result);
  } catch {
    if (loadVersion !== rankingLoadVersion || requestedGame !== currentGame) return;
    rankingList.replaceChildren();
    rankingTable.hidden = true;
    rankingEmpty.hidden = false;
    rankingEmpty.querySelector("strong").textContent = "Não foi possível carregar o ranking.";
    rankingEmpty.querySelector("p").textContent = "Tente novamente quando o servidor estiver disponível.";
  }
}

async function loadMyRanking() {
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  const requestedGame = currentGame;
  const loadVersion = ++myRankingLoadVersion;
  myRanking.hidden = true;
  if (!token) return;
  try {
    const response = await fetch(`${API_BASE_URL}/rankings/${requestedGame}/me`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) return;
    const data = await response.json();
    if (loadVersion !== myRankingLoadVersion || requestedGame !== currentGame) return;
    myPosition.textContent = data.position == null ? "Ainda não ranqueado" : `#${data.position}`;
    myRating.textContent = `${data.rating} Elo`;
    myRanking.hidden = false;
  } catch {
    // O ranking público continua disponível sem a posição individual.
  }
}

function updateUrl() {
  const params = new URLSearchParams({ game: currentGame });
  if (currentPage > 0) params.set("page", String(currentPage));
  window.history.replaceState({}, "", `ranking.html?${params}`);
}

function updateGamePresentation() {
  const config = GAME_CONFIG[currentGame];
  document.title = config.title;
  rankingTitle.textContent = config.heading;
  rankingDescription.content = config.description;
  rankingEyebrow.textContent = `Competitivo · ${config.label}`;
  rankingTable.setAttribute("aria-label", `Ranking de ${config.label}`);
  gameButtons.forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.rankingGame === currentGame));
  });
}

function selectGame(game) {
  if (!GAME_CONFIG[game] || game === currentGame) return;
  currentGame = game;
  currentPage = 0;
  rankingList.replaceChildren();
  rankingTable.hidden = true;
  rankingEmpty.hidden = true;
  updateGamePresentation();
  updateUrl();
  loadRanking(0);
  loadMyRanking();
}

previousButton.addEventListener("click", () => loadRanking(currentPage - 1));
nextButton.addEventListener("click", () => loadRanking(currentPage + 1));
gameButtons.forEach((button) => {
  button.addEventListener("click", () => selectGame(button.dataset.rankingGame));
});

window.lucide?.createIcons();
updateGamePresentation();
updateUrl();
loadRanking();
loadMyRanking();
