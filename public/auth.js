const API_BASE_URL = "http://localhost:8080/api/v1";
const TOKEN_STORAGE_KEY = "duelo64.accessToken";
const USER_STORAGE_KEY = "duelo64.user";

function getSafeReturnUrl() {
  const requestedDestination = new URLSearchParams(window.location.search).get("returnTo");

  if (!requestedDestination) {
    return new URL("index.html", window.location.href).href;
  }

  try {
    const destination = new URL(requestedDestination, window.location.href);
    const currentDirectory = new URL("./", window.location.href);
    const staysInsideApplication = destination.href.startsWith(currentDirectory.href);
    const usesAllowedProtocol = destination.protocol === "http:" || destination.protocol === "https:";

    if (!staysInsideApplication || !usesAllowedProtocol) {
      return new URL("index.html", window.location.href).href;
    }

    return destination.href;
  } catch {
    return new URL("index.html", window.location.href).href;
  }
}

const state = {
  email: "",
  token: sessionStorage.getItem(TOKEN_STORAGE_KEY) || "",
};

const steps = [...document.querySelectorAll("[data-auth-step]")];
const requestCodeForm = document.querySelector("#request-code-form");
const verifyCodeForm = document.querySelector("#verify-code-form");
const completeProfileForm = document.querySelector("#complete-profile-form");
const emailInput = document.querySelector("#auth-email");
const codeInput = document.querySelector("#auth-code");
const nicknameInput = document.querySelector("#auth-nickname");
const codeRecipient = document.querySelector("#code-recipient");
const emailError = document.querySelector("#email-error");
const codeError = document.querySelector("#code-error");
const nicknameError = document.querySelector("#nickname-error");
const nicknameAvailability = document.querySelector("#nickname-availability");
const authStatus = document.querySelector("#auth-status");
const resendCodeButton = document.querySelector("#resend-code-button");
const changeEmailButton = document.querySelector("#change-email-button");
let nicknameAvailabilityTimeout = null;

function refreshIcons() {
  if (window.lucide) {
    window.lucide.createIcons();
  }
}

function showStep(stepName) {
  steps.forEach((step) => {
    step.hidden = step.dataset.authStep !== stepName;
  });

  setStatus("");

  const activeInput = document.querySelector(
    `[data-auth-step="${stepName}"] input`,
  );
  activeInput?.focus();
}

function setStatus(message, type = "") {
  authStatus.textContent = message;
  authStatus.classList.toggle("is-error", type === "error");
  authStatus.classList.toggle("is-success", type === "success");
}

function setFieldError(input, errorElement, message = "") {
  errorElement.textContent = message;

  if (message) {
    input.setAttribute("aria-invalid", "true");
  } else {
    input.removeAttribute("aria-invalid");
  }
}

function setNicknameAvailability(message = "", type = "") {
  if (!nicknameAvailability) return;

  nicknameAvailability.textContent = message;
  nicknameAvailability.classList.toggle("is-unavailable", type === "unavailable");
  nicknameAvailability.classList.toggle("is-invalid", type === "invalid");
}

function setButtonLoading(button, loading, loadingText) {
  if (!button.dataset.defaultContent) {
    button.dataset.defaultContent = button.innerHTML;
  }

  button.disabled = loading;
  button.setAttribute("aria-busy", String(loading));
  button.innerHTML = loading ? loadingText : button.dataset.defaultContent;
  refreshIcons();
}

function maskEmail(email) {
  const [name, domain] = email.split("@");

  if (!name || !domain) {
    return email;
  }

  return `${name.charAt(0)}${"*".repeat(Math.min(name.length - 1, 4))}@${domain}`;
}

async function apiRequest(path, options = {}) {
  let response;

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
    });
  } catch {
    throw new Error("Não foi possível conectar ao servidor. Verifique se o backend está ligado.");
  }

  const contentType = response.headers.get("content-type") || "";
  const responseBody = contentType.includes("application/json")
    ? await response.json()
    : null;

  if (!response.ok) {
    const error = new Error(
      responseBody?.message || "Não foi possível concluir a solicitação.",
    );
    error.code = responseBody?.code;
    error.status = response.status;
    throw error;
  }

  return responseBody;
}

async function requestCode(email) {
  await apiRequest("/auth/codes", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

function saveSession(loginResponse) {
  state.token = loginResponse.accessToken;
  sessionStorage.setItem(TOKEN_STORAGE_KEY, loginResponse.accessToken);
  sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(loginResponse.user));
}

function saveUser(user) {
  sessionStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
}

function finishAuthentication() {
  setStatus("Acesso confirmado. Entrando no Duelo64...", "success");

  window.setTimeout(() => {
    window.location.href = getSafeReturnUrl();
  }, 650);
}

requestCodeForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const email = emailInput.value.trim().toLowerCase();
  const submitButton = requestCodeForm.querySelector("button[type='submit']");
  setFieldError(emailInput, emailError);

  if (!email || !emailInput.checkValidity()) {
    setFieldError(emailInput, emailError, "Informe um e-mail válido.");
    emailInput.focus();
    return;
  }

  setButtonLoading(submitButton, true, "Enviando...");
  setStatus("");

  try {
    await requestCode(email);
    state.email = email;
    codeRecipient.textContent = maskEmail(email);
    showStep("code");
    setStatus("Código enviado. Confira sua caixa de entrada.", "success");
  } catch (error) {
    setStatus(error.message, "error");
  } finally {
    setButtonLoading(submitButton, false, "");
  }
});

codeInput?.addEventListener("input", () => {
  codeInput.value = codeInput.value.replace(/\D/g, "").slice(0, 6);
  setFieldError(codeInput, codeError);
});

verifyCodeForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const code = codeInput.value.trim();
  const submitButton = verifyCodeForm.querySelector("button[type='submit']");
  setFieldError(codeInput, codeError);

  if (!/^\d{6}$/.test(code)) {
    setFieldError(codeInput, codeError, "Digite os 6 dígitos enviados por e-mail.");
    codeInput.focus();
    return;
  }

  setButtonLoading(submitButton, true, "Confirmando...");
  setStatus("");

  try {
    const loginResponse = await apiRequest("/auth/codes/verify", {
      method: "POST",
      body: JSON.stringify({
        email: state.email,
        code,
      }),
    });

    saveSession(loginResponse);

    if (loginResponse.user.nickname) {
      finishAuthentication();
      return;
    }

    showStep("profile");
    setStatus("E-mail confirmado. Agora escolha seu nickname.", "success");
  } catch (error) {
    setFieldError(codeInput, codeError, error.message);
  } finally {
    setButtonLoading(submitButton, false, "");
  }
});

resendCodeButton?.addEventListener("click", async () => {
  if (!state.email) {
    showStep("email");
    return;
  }

  resendCodeButton.disabled = true;
  setStatus("Reenviando código...");

  try {
    await requestCode(state.email);
    setStatus("Um novo código foi enviado.", "success");
  } catch (error) {
    setStatus(error.message, "error");
  } finally {
    resendCodeButton.disabled = false;
  }
});

changeEmailButton?.addEventListener("click", () => {
  state.email = "";
  codeInput.value = "";
  setFieldError(codeInput, codeError);
  showStep("email");
});

nicknameInput?.addEventListener("input", () => {
  const nickname = nicknameInput.value.trim();

  setFieldError(nicknameInput, nicknameError);
  setNicknameAvailability("");
  window.clearTimeout(nicknameAvailabilityTimeout);

  if (!nickname) return;

  if (!/^[A-Za-z0-9_]{3,24}$/.test(nickname)) {
    setNicknameAvailability("Use apenas letras, numeros ou underline.", "invalid");
    return;
  }

  nicknameAvailabilityTimeout = window.setTimeout(async () => {
    try {
      const response = await apiRequest(
        `/users/nicknames/${encodeURIComponent(nickname)}/availability`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${state.token}`,
          },
        },
      );

      if (response.available) {
        setNicknameAvailability("Disponivel.");
        return;
      }

      setNicknameAvailability("Esse nickname ja esta em uso.", "unavailable");
    } catch {
      setNicknameAvailability("");
    }
  }, 450);
});

completeProfileForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const nickname = nicknameInput.value.trim();
  const submitButton = completeProfileForm.querySelector("button[type='submit']");
  setFieldError(nicknameInput, nicknameError);

  if (!/^[A-Za-z0-9_]{3,24}$/.test(nickname)) {
    setFieldError(
      nicknameInput,
      nicknameError,
      "Use de 3 a 24 letras, números ou underline.",
    );
    nicknameInput.focus();
    return;
  }

  if (!state.token) {
    setStatus("Sua sessão não foi encontrada. Confirme o e-mail novamente.", "error");
    showStep("email");
    return;
  }

  setButtonLoading(submitButton, true, "Salvando...");
  setStatus("");

  try {
    const user = await apiRequest("/users/me/profile", {
      method: "PATCH",
      headers: {
        Authorization: `Bearer ${state.token}`,
      },
      body: JSON.stringify({ nickname }),
    });

    saveUser(user);
    finishAuthentication();
  } catch (error) {
    if (error.code === "NICKNAME_UNAVAILABLE") {
      setFieldError(nicknameInput, nicknameError, error.message);
      nicknameInput.focus();
    } else {
      setStatus(error.message, "error");
    }
  } finally {
    setButtonLoading(submitButton, false, "");
  }
});

async function restoreSession() {
  if (!state.token) {
    return;
  }

  try {
    const user = await apiRequest("/users/me", {
      method: "GET",
      headers: {
        Authorization: `Bearer ${state.token}`,
      },
    });

    saveUser(user);

    if (user.nickname) {
      finishAuthentication();
    } else {
      showStep("profile");
    }
  } catch {
    state.token = "";
    sessionStorage.removeItem(TOKEN_STORAGE_KEY);
    sessionStorage.removeItem(USER_STORAGE_KEY);
  }
}

refreshIcons();
restoreSession();
