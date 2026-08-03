(() => {
  const API_BASE_URL = "http://localhost:8080/api/v1";
  const TOKEN_STORAGE_KEY = "duelo64.accessToken";
  const USER_STORAGE_KEY = "duelo64.user";

  function refreshIcons() {
    window.lucide?.createIcons();
  }

  function readCachedUser() {
    try {
      return JSON.parse(sessionStorage.getItem(USER_STORAGE_KEY));
    } catch {
      sessionStorage.removeItem(USER_STORAGE_KEY);
      return null;
    }
  }

  function clearSession() {
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
  }

  function normalizeNickname(nickname) {
    return (nickname || "jogador").replace(/^@/, "");
  }

  function initializeAccount(root) {
    const loginLink = root.querySelector("[data-account-login]");
    const authenticatedArea = root.querySelector("[data-account-auth]");
    const accountButton = root.querySelector("[data-account-button]");
    const accountPanel = root.querySelector("[data-account-panel]");
    const accountAvatar = root.querySelector("[data-account-avatar]");
    const accountNickname = root.querySelector("[data-account-nickname]");
    const menuNickname = root.querySelector("[data-account-menu-nickname]");
    const accountEmail = root.querySelector("[data-account-email]");
    const profileLink = root.querySelector("[data-account-profile]");
    const logoutButton = root.querySelector("[data-account-logout]");

    const loginUrl = root.dataset.loginUrl || "entrar.html";
    const profileUrl = root.dataset.profileUrl || "perfil.html";
    const homeUrl = root.dataset.homeUrl || "index.html";

    loginLink.href = loginUrl;
    profileLink.href = profileUrl;

    function closeMenu() {
      accountPanel.hidden = true;
      accountButton.setAttribute("aria-expanded", "false");
    }

    function showLoggedOut() {
      loginLink.hidden = false;
      authenticatedArea.hidden = true;
      closeMenu();
    }

    function showUser(user) {
      const nickname = normalizeNickname(user.nickname);
      const nicknameLabel = `@${nickname}`;
      const initial = nickname.charAt(0).toUpperCase() || "J";

      loginLink.hidden = true;
      authenticatedArea.hidden = false;
      accountNickname.textContent = nicknameLabel;
      menuNickname.textContent = nicknameLabel;
      accountEmail.textContent = user.email || "";

      if (user.avatarUrl) {
        const image = document.createElement("img");
        image.src = user.avatarUrl;
        image.alt = `Avatar de ${nicknameLabel}`;
        accountAvatar.replaceChildren(image);
      } else {
        accountAvatar.textContent = initial;
      }

      refreshIcons();
    }

    async function refreshUser(token) {
      let response;

      try {
        response = await fetch(`${API_BASE_URL}/users/me`, {
          headers: { Authorization: `Bearer ${token}` },
        });
      } catch {
        return;
      }

      if (response.status === 401 || response.status === 403) {
        clearSession();
        showLoggedOut();
        window.dispatchEvent(new CustomEvent("duelo64:session-expired"));
        return;
      }

      if (!response.ok) return;

      const user = await response.json();
      sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
      showUser(user);
    }

    accountButton.addEventListener("click", (event) => {
      event.stopPropagation();
      const willOpen = accountPanel.hidden;
      accountPanel.hidden = !willOpen;
      accountButton.setAttribute("aria-expanded", String(willOpen));
    });

    logoutButton.addEventListener("click", () => {
      clearSession();
      window.location.href = homeUrl;
    });

    document.addEventListener("click", (event) => {
      if (!root.contains(event.target)) closeMenu();
    });

    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape" && !accountPanel.hidden) {
        closeMenu();
        accountButton.focus();
      }
    });

    const token = sessionStorage.getItem(TOKEN_STORAGE_KEY);
    const cachedUser = readCachedUser();

    if (!token) {
      showLoggedOut();
      return;
    }

    if (cachedUser?.nickname) showUser(cachedUser);
    else showLoggedOut();

    refreshUser(token);
  }

  document.querySelectorAll("[data-shared-account]").forEach(initializeAccount);
  refreshIcons();
})();
