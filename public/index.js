const { API_BASE_URL } = window.Duelo64Config;
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const USER_STORAGE_KEY = "duelo64.user";

const header = document.querySelector(".site-header");
const menuButton = document.querySelector(".menu-button");
const headerAuthLink = document.querySelector("#header-auth-link");
const mobileAuthLink = document.querySelector("#mobile-auth-link");
const mobileAccountAvatar = document.querySelector("#mobile-account-avatar");
const mobileAccountCaption = document.querySelector("#mobile-account-caption");
const mobileAccountName = document.querySelector("#mobile-account-name");
const mobileAccountChevron = document.querySelector("#mobile-account-chevron");
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
    mobileAuthLink.href = "entrar.html";
    mobileAuthLink.classList.remove("authenticated");
  }

  if (mobileAccountAvatar) {
    mobileAccountAvatar.hidden = true;
    mobileAccountAvatar.textContent = "J";
  }

  if (mobileAccountCaption) {
    mobileAccountCaption.hidden = true;
  }

  if (mobileAccountName) {
    mobileAccountName.textContent = "Entrar no Duelo";
  }

  if (mobileAccountChevron) {
    mobileAccountChevron.hidden = true;
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
    if (user.avatarUrl) {
      const image = document.createElement("img");
      image.src = user.avatarUrl;
      image.alt = "";
      userAccountAvatar.replaceChildren(image);
    } else {
      userAccountAvatar.textContent = avatarLetter;
    }
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
    mobileAuthLink.href = "perfil.html";
    mobileAuthLink.classList.add("authenticated");
  }

  if (mobileAccountAvatar) {
    mobileAccountAvatar.hidden = false;

    if (user.avatarUrl) {
      const image = document.createElement("img");
      image.src = user.avatarUrl;
      image.alt = "";
      mobileAccountAvatar.replaceChildren(image);
    } else {
      mobileAccountAvatar.textContent = avatarLetter;
    }
  }

  if (mobileAccountCaption) {
    mobileAccountCaption.hidden = false;
  }

  if (mobileAccountName) {
    mobileAccountName.textContent = nicknameLabel;
  }

  if (mobileAccountChevron) {
    mobileAccountChevron.hidden = false;
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
