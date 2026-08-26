import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { initializeAuthentication } from "./auth";
import "./index.css";

const root = document.getElementById("root");

if (root === null) {
  throw new Error("Application root element was not found");
}
const applicationRoot: HTMLElement = root;

async function bootstrap() {
  try {
    const auth = await initializeAuthentication();
    createRoot(applicationRoot).render(
      <StrictMode>
        <App auth={auth} />
      </StrictMode>
    );
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : "Authentication could not be initialized";
    createRoot(applicationRoot).render(
      <main className="grid min-h-screen place-items-center bg-[#f4f1ea] p-6 text-[#17201f]">
        <section className="max-w-lg border border-[#b94327] bg-white p-8 shadow-[4px_4px_0_#b94327]">
          <h1 className="text-2xl font-semibold">Authentication unavailable</h1>
          <p className="mt-3 text-sm leading-6 text-[#66716f]">{message}</p>
        </section>
      </main>
    );
  }
}

void bootstrap();
