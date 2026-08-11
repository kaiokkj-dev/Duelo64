const API_BASE_URL = "http://localhost:8080/api/v1";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const USER_STORAGE_KEY = "duelo64.user";
const requestedProfileId = new URLSearchParams(window.location.search).get("id");

function readStoredUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_STORAGE_KEY));
  } catch {
    return null;
  }
}

const signedInUserId = readStoredUser()?.id || null;
const isPublicProfileMode = Boolean(requestedProfileId && requestedProfileId !== signedInUserId);

const profileAvatar = document.querySelector("#profile-avatar");
const profileTitle = document.querySelector("#profile-title");
const profileEmail = document.querySelector("#profile-email");
const logoutButton = document.querySelector("#profile-logout-button");
const editButton = document.querySelector("#profile-edit-button");
const editor = document.querySelector("#profile-editor");
const editorCloseButton = document.querySelector("#editor-close-button");
const editorCancelButton = document.querySelector("#editor-cancel-button");
const profileForm = document.querySelector("#profile-form");
const nicknameInput = document.querySelector("#nickname-input");
const avatarGrid = document.querySelector("#avatar-grid");
const formMessage = document.querySelector("#profile-form-message");
const saveButton = document.querySelector("#editor-save-button");
const avatarFileInput = document.querySelector("#avatar-file-input");
const avatarPreview = document.querySelector("#custom-avatar-preview");
const avatarPreviewImage = document.querySelector("#custom-avatar-preview-image");
const avatarFileName = document.querySelector("#custom-avatar-file-name");
const historyList = document.querySelector("#history-list");
const historyEmpty = document.querySelector("#history-empty");
const profileRating = document.querySelector("#profile-ranking");
const profileMatches = document.querySelector("#profile-matches");
const profileWins = document.querySelector("#profile-wins");
const profileLosses = document.querySelector("#profile-losses");
const profileDraws = document.querySelector("#profile-draws");
const profileWinRate = document.querySelector("#profile-win-rate");
const profileRatingLabel = document.querySelector("#profile-rating-label");
const gameTabs = [...document.querySelectorAll(".profile-game-tab")];

const AVATARS = ["Blaze", "Byte", "Dash", "Echo", "Flux", "Nova", "Pixel", "Volt"].map(
  (seed) => ({
    seed,
    url: `https://api.dicebear.com/10.x/bottts-neutral/svg?seed=${seed}`,
  }),
);

let currentUser = null;
let previewObjectUrl = null;
let selectedGameType = "CHECKERS";
let gameDataRequestId = 0;

function redirectToLogin() {
  window.location.replace("entrar.html");
}

function clearSession() {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  sessionStorage.removeItem(USER_STORAGE_KEY);
}

function renderUser(user, publicView = false) {
  const nickname = user.nickname || "jogador";

  profileTitle.textContent = `@${nickname}`;
  profileEmail.textContent = publicView ? "Perfil público de jogador" : user.email;

  if (user.avatarUrl) {
    const image = document.createElement("img");
    image.src = user.avatarUrl;
    image.alt = `Avatar de ${nickname}`;
    profileAvatar.replaceChildren(image);
  } else {
    profileAvatar.textContent = nickname.charAt(0).toUpperCase() || "J";
  }

  document.body.classList.remove("profile-loading");
  currentUser = user;
  document.title = `${profileTitle.textContent} — Duelo64`;
}

function configurePublicView() {
  editButton.hidden = true;
  editor.hidden = true;
  if (!sessionStorage.getItem(TOKEN_STORAGE_KEY)) logoutButton.hidden = true;
}

function showPublicProfileError(message) {
  configurePublicView();
  profileAvatar.textContent = "?";
  profileTitle.textContent = "Jogador não encontrado";
  profileEmail.textContent = message;
  document.querySelector(".profile-stats").hidden = true;
  document.querySelector(".profile-history").hidden = true;
  document.body.classList.remove("profile-loading");
}

function renderAvatarOptions(selectedUrl) {
  avatarGrid.replaceChildren();

  AVATARS.forEach((avatar, index) => {
    const label = document.createElement("label");
    label.className = "avatar-option";

    const input = document.createElement("input");
    input.type = "radio";
    input.name = "avatarUrl";
    input.value = avatar.url;
    input.checked = selectedUrl ? avatar.url === selectedUrl : index === 0;
    input.setAttribute("aria-label", `Avatar ${avatar.seed}`);
    input.addEventListener("change", clearCustomAvatarSelection);

    const visual = document.createElement("span");
    visual.className = "avatar-option-visual";

    const image = document.createElement("img");
    image.src = avatar.url;
    image.alt = "";
    image.loading = "lazy";

    visual.append(image);
    label.append(input, visual);
    avatarGrid.append(label);
  });
}

function clearPreviewObjectUrl() {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl);
    previewObjectUrl = null;
  }
}

function clearCustomAvatarSelection() {
  clearPreviewObjectUrl();
  avatarFileInput.value = "";
  avatarPreviewImage.removeAttribute("src");
  avatarPreviewImage.hidden = true;
  avatarPreview.querySelector("svg")?.removeAttribute("hidden");
  avatarFileName.textContent = "Nenhum arquivo selecionado";
}

function selectCustomAvatar(file) {
  clearPreviewObjectUrl();

  document
    .querySelectorAll('input[name="avatarUrl"]')
    .forEach((input) => {
      input.checked = false;
    });

  previewObjectUrl = URL.createObjectURL(file);
  avatarPreviewImage.src = previewObjectUrl;
  avatarPreviewImage.hidden = false;
  avatarPreview.querySelector("svg")?.setAttribute("hidden", "");
  avatarFileName.textContent = file.name;
}

function validateAvatarFile(file) {
  const allowedTypes = ["image/jpeg", "image/png", "image/webp"];

  if (!allowedTypes.includes(file.type)) {
    return "Escolha uma imagem JPEG, PNG ou WebP.";
  }

  if (file.size > 2 * 1024 * 1024) {
    return "A imagem deve possuir no máximo 2 MB.";
  }

  return null;
}

function openEditor() {
  if (!currentUser) {
    return;
  }

  nicknameInput.value = currentUser.nickname || "";
  clearCustomAvatarSelection();
  renderAvatarOptions(currentUser.avatarUrl);
  formMessage.textContent = "";
  formMessage.classList.remove("success");
  editor.hidden = false;
  editButton.setAttribute("aria-expanded", "true");
  nicknameInput.focus();
  editor.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function closeEditor() {
  clearCustomAvatarSelection();
  editor.hidden = true;
  editButton.setAttribute("aria-expanded", "false");
  editButton.focus();
}

function setSaving(isSaving) {
  saveButton.disabled = isSaving;
  editorCancelButton.disabled = isSaving;
  editorCloseButton.disabled = isSaving;
  avatarFileInput.disabled = isSaving;
  saveButton.textContent = isSaving ? "Salvando..." : "Salvar alterações";
}

function validateNickname(nickname) {
  if (nickname.length < 3 || nickname.length > 24) {
    return "O nickname deve possuir entre 3 e 24 caracteres.";
  }

  if (!/^[A-Za-z0-9_]+$/.test(nickname)) {
    return "Use somente letras, números e underline no nickname.";
  }

  return null;
}

async function loadProfile() {
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);

  if (!token) {
    redirectToLogin();
    return;
  }

  document.body.classList.add("profile-loading");

  try {
    const response = await fetch(`${API_BASE_URL}/users/me`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (response.status === 401 || response.status === 403) {
      clearSession();
      redirectToLogin();
      return;
    }

    if (!response.ok) {
      throw new Error("Não foi possível carregar o perfil.");
    }

    const user = await response.json();

    if (!user.nickname) {
      window.location.replace("entrar.html");
      return;
    }

    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
    renderUser(user);
  } catch {
    const cachedUser = sessionStorage.getItem(USER_STORAGE_KEY);

    if (!cachedUser) {
      profileEmail.textContent = "Não foi possível carregar o perfil agora.";
      document.body.classList.remove("profile-loading");
      return;
    }

    try {
      renderUser(JSON.parse(cachedUser));
    } catch {
      clearSession();
      redirectToLogin();
    }
  }
}

async function loadPublicProfile(gameType = selectedGameType, requestId = gameDataRequestId) {
  document.body.classList.add("profile-loading");
  configurePublicView();

  try {
    const response = await fetch(
      `${API_BASE_URL}/users/${encodeURIComponent(requestedProfileId)}/public-profile?gameType=${gameType}`,
    );
    const data = await response.json().catch(() => ({}));
    if (response.status === 404) {
      showPublicProfileError(data.message || "Esse jogador não existe ou não está mais disponível.");
      return;
    }
    if (!response.ok) throw new Error(data.message || "Não foi possível carregar este perfil.");

    if (requestId !== gameDataRequestId) return;
    renderUser(data, true);
    renderStats(data);
    renderHistory(data.recentMatches || []);
  } catch (error) {
    if (requestId === gameDataRequestId) showPublicProfileError(error.message);
  }
}

async function loadHistory(gameType = selectedGameType, requestId = gameDataRequestId) {
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  if (!token || !historyList || !historyEmpty) return;

  try {
    const response = await fetch(`${API_BASE_URL}/matches/me?gameType=${gameType}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) throw new Error();
    const matches = await response.json();
    if (requestId === gameDataRequestId) renderHistory(matches);
  } catch {
    historyEmpty.querySelector("strong").textContent = "Não foi possível carregar seu histórico.";
    historyEmpty.querySelector("p").textContent = "Tente novamente quando o servidor estiver disponível.";
  }
}

function renderHistory(matches) {
  historyList.replaceChildren();
  historyEmpty.hidden = matches.length > 0;
  historyList.hidden = matches.length === 0;

  matches.forEach((match) => {
    const card = document.createElement("article");
    card.className = `history-match ${match.result.toLowerCase()}`;

    const linksToOpponent = Boolean(match.opponentId && match.opponentId !== signedInUserId);
    const avatar = document.createElement(linksToOpponent ? "a" : "span");
    avatar.className = "history-opponent-avatar";
    if (linksToOpponent) {
      avatar.href = `perfil.html?id=${encodeURIComponent(match.opponentId)}`;
      avatar.setAttribute("aria-label", `Abrir perfil de @${match.opponentNickname || "jogador"}`);
    }
    if (match.opponentAvatarUrl) {
      const image = document.createElement("img");
      image.src = match.opponentAvatarUrl;
      image.alt = "";
      avatar.appendChild(image);
    } else {
      avatar.textContent = (match.opponentNickname || "R").charAt(0).toUpperCase();
    }

    const copy = document.createElement("div");
    copy.className = "history-match-copy";
    const opponent = document.createElement("strong");
    const opponentLabel = `vs @${match.opponentNickname || "jogador"}`;
    if (linksToOpponent) {
      const opponentLink = document.createElement("a");
      opponentLink.className = "history-opponent-link";
      opponentLink.href = `perfil.html?id=${encodeURIComponent(match.opponentId)}`;
      opponentLink.textContent = opponentLabel;
      opponent.appendChild(opponentLink);
    } else {
      opponent.textContent = opponentLabel;
    }
    const details = document.createElement("small");
    details.textContent = `${match.playerColor === "WHITE" ? "Brancas" : "Pretas"} · ${match.timeControlMinutes} min · ${match.moveCount} movimentos`;
    copy.append(opponent, details);

    const result = document.createElement("div");
    result.className = "history-result";
    const resultLabel = document.createElement("strong");
    resultLabel.textContent = match.result === "WIN" ? "Vitória" : match.result === "LOSS" ? "Derrota" : "Empate";
    result.appendChild(resultLabel);
    if (match.matchType === "RANKED" && Number.isInteger(match.ratingChange)) {
      const ratingChange = document.createElement("span");
      ratingChange.className = "history-rating-change";
      ratingChange.classList.add(match.ratingChange > 0 ? "positive" : match.ratingChange < 0 ? "negative" : "neutral");
      ratingChange.textContent = `${match.ratingChange > 0 ? "+" : ""}${match.ratingChange} Elo`;
      result.appendChild(ratingChange);
    }
    const date = document.createElement("time");
    date.dateTime = match.finishedAt;
    date.textContent = formatMatchDate(match.finishedAt);
    result.appendChild(date);

    card.append(avatar, copy, result);
    historyList.appendChild(card);
  });
}

async function loadStats(gameType = selectedGameType, requestId = gameDataRequestId) {
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
  if (!token) return;

  try {
    const response = await fetch(`${API_BASE_URL}/stats/me?gameType=${gameType}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) throw new Error();
    const stats = await response.json();
    if (requestId === gameDataRequestId) renderStats(stats);
  } catch {
    profileRating.textContent = "—";
  }
}

function renderStats(stats) {
  profileRating.textContent = stats.rating;
  profileMatches.textContent = stats.gamesPlayed;
  profileWins.textContent = stats.wins;
  profileLosses.textContent = stats.losses;
  profileDraws.textContent = stats.draws;
  profileWinRate.textContent = `${stats.winRate.toLocaleString("pt-BR", { maximumFractionDigits: 2 })}%`;
}

function selectGameType(gameType) {
  selectedGameType = gameType;
  profileRatingLabel.textContent = gameType === "CHESS" ? "Elo · Xadrez" : "Elo · Damas";
  gameTabs.forEach((tab) => {
    const active = tab.dataset.gameType === gameType;
    tab.classList.toggle("active", active);
    tab.setAttribute("aria-pressed", String(active));
  });

  const requestId = ++gameDataRequestId;
  if (isPublicProfileMode) {
    loadPublicProfile(gameType, requestId);
  } else {
    loadStats(gameType, requestId);
    loadHistory(gameType, requestId);
  }
}

function formatMatchDate(value) {
  const date = new Date(value);
  const today = new Date();
  const sameDay = date.toDateString() === today.toDateString();
  return sameDay
    ? `Hoje, ${date.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}`
    : date.toLocaleDateString("pt-BR");
}

logoutButton.addEventListener("click", () => {
  clearSession();
  window.location.replace("index.html");
});

gameTabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    if (tab.dataset.gameType !== selectedGameType) selectGameType(tab.dataset.gameType);
  });
});

editButton.addEventListener("click", () => {
  if (editor.hidden) {
    openEditor();
  } else {
    closeEditor();
  }
});

editorCloseButton.addEventListener("click", closeEditor);
editorCancelButton.addEventListener("click", closeEditor);

avatarFileInput.addEventListener("change", () => {
  const file = avatarFileInput.files[0];

  formMessage.textContent = "";
  formMessage.classList.remove("success");

  if (!file) {
    clearCustomAvatarSelection();
    return;
  }

  const fileError = validateAvatarFile(file);

  if (fileError) {
    clearCustomAvatarSelection();
    formMessage.textContent = fileError;
    return;
  }

  selectCustomAvatar(file);
});

profileForm.addEventListener("submit", async (event) => {
  event.preventDefault();

  const nickname = nicknameInput.value.trim();
  const nicknameError = validateNickname(nickname);

  if (nicknameError) {
    formMessage.textContent = nicknameError;
    nicknameInput.focus();
    return;
  }

  const selectedAvatar = profileForm.elements.avatarUrl.value || null;
  const customAvatarFile = avatarFileInput.files[0] || null;
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);

  if (customAvatarFile) {
    const fileError = validateAvatarFile(customAvatarFile);

    if (fileError) {
      formMessage.textContent = fileError;
      return;
    }
  }

  if (!token) {
    redirectToLogin();
    return;
  }

  setSaving(true);
  formMessage.textContent = "";
  formMessage.classList.remove("success");

  try {
    const response = await fetch(`${API_BASE_URL}/users/me/profile`, {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ nickname, avatarUrl: selectedAvatar }),
    });

    let data = await response.json().catch(() => ({}));

    if (response.status === 401 || response.status === 403) {
      clearSession();
      redirectToLogin();
      return;
    }

    if (!response.ok) {
      throw new Error(data.message || "Não foi possível salvar o perfil.");
    }

    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(data));
    renderUser(data);

    if (customAvatarFile) {
      formMessage.textContent = "Enviando sua foto...";

      const avatarFormData = new FormData();
      avatarFormData.append("avatar", customAvatarFile);

      const avatarResponse = await fetch(`${API_BASE_URL}/users/me/avatar`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: avatarFormData,
      });

      const avatarData = await avatarResponse.json().catch(() => ({}));

      if (avatarResponse.status === 401 || avatarResponse.status === 403) {
        clearSession();
        redirectToLogin();
        return;
      }

      if (!avatarResponse.ok) {
        throw new Error(
          avatarData.message ||
            "O nickname foi salvo, mas não foi possível enviar a foto.",
        );
      }

      data = avatarData;
    }

    sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(data));
    renderUser(data);
    formMessage.textContent = "Perfil atualizado com sucesso.";
    formMessage.classList.add("success");

    window.setTimeout(closeEditor, 700);
  } catch (error) {
    formMessage.textContent = error.message;
  } finally {
    setSaving(false);
  }
});

if (window.lucide) {
  window.lucide.createIcons();
}

if (!isPublicProfileMode) loadProfile();
selectGameType("CHECKERS");
