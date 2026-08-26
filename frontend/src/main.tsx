import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { hasAuthenticationCallback, initializeAuthentication, login, registerCandidate } from "./auth";
import "./index.css";

const root = document.getElementById("root");

if (root === null) {
  throw new Error("Application root element was not found");
}
const applicationRoot: HTMLElement = root;

async function bootstrap() {
  try {
    const auth = hasAuthenticationCallback(window.location) ? await initializeAuthentication() : null;
    createRoot(applicationRoot).render(
      <StrictMode>
        <App auth={auth} onLogin={login} onRegisterCandidate={registerCandidate} />
      </StrictMode>
    );
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Authentication could not be initialized";
    createRoot(applicationRoot).render(
      <StrictMode>
        <App auth={null} authError={message} onLogin={login} onRegisterCandidate={registerCandidate} />
      </StrictMode>
    );
  }
}

void bootstrap();
