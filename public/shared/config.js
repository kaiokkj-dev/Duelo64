(function configureDuelo64() {
  "use strict";

  const LOCAL_BACKEND_URL = "http://localhost:8080";

  // Depois de publicar o backend, coloque aqui a URL HTTPS fornecida pelo Koyeb.
  // Exemplo: "https://duelo64-api-seuusuario.koyeb.app"
  const PRODUCTION_BACKEND_URL = "";

  const isLocalFrontend = window.location.protocol === "file:"
    || ["localhost", "127.0.0.1"].includes(window.location.hostname);
  const backendUrl = (isLocalFrontend ? LOCAL_BACKEND_URL : PRODUCTION_BACKEND_URL).replace(/\/+$/, "");

  if (!backendUrl) {
    throw new Error("Configure PRODUCTION_BACKEND_URL em public/shared/config.js.");
  }

  const websocketProtocol = backendUrl.startsWith("https://") ? "wss://" : "ws://";
  const websocketHost = backendUrl.replace(/^https?:\/\//, "");

  window.Duelo64Config = Object.freeze({
    BACKEND_URL: backendUrl,
    API_BASE_URL: `${backendUrl}/api/v1`,
    WS_BASE_URL: `${websocketProtocol}${websocketHost}/ws`,
  });
})();
