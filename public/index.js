const API_BASE_URL = "http://localhost:8080/api/v1";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const USER_STORAGE_KEY = "duelo64.user";

const header = document.querySelector(".site-header");
const menuButton = document.querySelector(".menu-button");
const headerAuthLink = document.querySelector("#header-auth-link");
const mobileAuthLink = document.querySelector("#mobile-auth-link");
const mobileLogoutButton = document.querySelector("#mobile-logout-button");
const userAccount = document.querySelector("#user-account");
const userAccountButton = document.querySelector("#user-account-button");
const userAccountMenu = document.querySelector("#user-account-menu");
const userAccountAvatar = document.querySelector("#user-account-avatar");
const userAccountNickname = document.querySelector("#user-account-nickname");
const userMenuNickname = document.querySelector("#user-menu-nickname");
const userMenuEmail = document.querySelector("#user-menu-email");
const userLogoutButton = document.querySelector("#user-logout-button");

function refreshIcons() {
  if (window.lucide) {
    window.lucide.createIcons();
  }
}

function closeNavigationMenu() {
  header?.classList.remove("menu-open");
  menuButton?.setAttribute("aria-expanded", "false");
}

function closeUserMenu() {
  if (userAccountMenu) {
    userAccountMenu.hidden = true;
  }

  userAccountButton?.setAttribute("aria-expanded", "false");
}

function readStoredUser() {
  const storedUser = sessionStorage.getItem(USER_STORAGE_KEY);

  if (!storedUser) {
    return null;
  }

  try {
    return JSON.parse(storedUser);
  } catch {
    sessionStorage.removeItem(USER_STORAGE_KEY);
    return null;
  }
}

function showLoggedOutState() {
  if (headerAuthLink) {
    headerAuthLink.hidden = false;
  }

  if (userAccount) {
    userAccount.hidden = true;
  }

  if (mobileAuthLink) {
    mobileAuthLink.textContent = "Entrar no Duelo";
    mobileAuthLink.href = "entrar.html";
  }

  if (mobileLogoutButton) {
    mobileLogoutButton.hidden = true;
  }

  closeUserMenu();
}

function showAuthenticatedState(user) {
  const nickname = user.nickname || "jogador";
  const nicknameLabel = `@${nickname}`;
  const avatarLetter = nickname.charAt(0).toUpperCase() || "J";

  if (headerAuthLink) {
    headerAuthLink.hidden = true;
  }

  if (userAccount) {
    userAccount.hidden = false;
  }

  if (userAccountAvatar) {
    userAccountAvatar.textContent = avatarLetter;
  }

  if (userAccountNickname) {
    userAccountNickname.textContent = nicknameLabel;
  }

  if (userMenuNickname) {
    userMenuNickname.textContent = nicknameLabel;
  }

  if (userMenuEmail) {
    userMenuEmail.textContent = user.email;
  }

  if (mobileAuthLink) {
    mobileAuthLink.textContent = nicknameLabel;
    mobileAuthLink.removeAttribute("href");
  }

  if (mobileLogoutButton) {
    mobileLogoutButton.hidden = false;
  }

  refreshIcons();
}

function clearSession() {
  sessionStorage.removeItem(TOKEN_STORAGE_KEY);
  sessionStorage.removeItem(USER_STORAGE_KEY);
  showLoggedOutState();
}

async function loadCurrentUser(token) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}/users/me`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  } catch {
    return null;
  }

  if (response.status === 401 || response.status === 403) {
    clearSession();
    return null;
  }

  if (!response.ok) {
    return null;
  }

  const user = await response.json();
  sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));

  return user;
}

async function restoreSession() {
  const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);

  if (!token) {
    showLoggedOutState();
    return;
  }

  const cachedUser = readStoredUser();

  if (cachedUser?.nickname) {
    showAuthenticatedState(cachedUser);
  }

  const currentUser = await loadCurrentUser(token);

  if (!currentUser) {
    if (!cachedUser) {
      showLoggedOutState();
    }
    return;
  }

  if (!currentUser.nickname) {
    window.location.href = "entrar.html";
    return;
  }

  showAuthenticatedState(currentUser);
}

menuButton?.addEventListener("click", () => {
  const isOpen = header.classList.toggle("menu-open");
  menuButton.setAttribute("aria-expanded", String(isOpen));
  closeUserMenu();
});

document.querySelectorAll(".top-navigation a").forEach((link) => {
  link.addEventListener("click", closeNavigationMenu);
});

userAccountButton?.addEventListener("click", (event) => {
  event.stopPropagation();

  const willOpen = userAccountMenu.hidden;
  userAccountMenu.hidden = !willOpen;
  userAccountButton.setAttribute("aria-expanded", String(willOpen));
  closeNavigationMenu();
});

function logout() {
  clearSession();
  closeNavigationMenu();
  window.location.href = "index.html";
}

userLogoutButton?.addEventListener("click", logout);
mobileLogoutButton?.addEventListener("click", logout);

mobileAuthLink?.addEventListener("click", (event) => {
  if (!mobileAuthLink.hasAttribute("href")) {
    event.preventDefault();
  }
});

document.addEventListener("click", (event) => {
  if (header && !header.contains(event.target)) {
    closeNavigationMenu();
  }

  if (userAccount && !userAccount.contains(event.target)) {
    closeUserMenu();
  }
});

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeNavigationMenu();
    closeUserMenu();
    menuButton?.focus();
  }
});

refreshIcons();
restoreSession();
