# Decisions Log

Running record of the choices made while building this sandbox and why. Each entry is a decision point, not a changelog of every file — mechanical steps (adding a dependency, a getter, a config line) aren't logged here.

---

## 2026-08-06 — Repo layout: flat at repo root, not nested under `eidas-wallet-sandbox/`

The original project brief nests all modules under an `eidas-wallet-sandbox/` subfolder inside a monorepo. Since this repo (`eWalletING`) *is* the sandbox — there's no outer project it lives alongside — nesting would just add a redundant folder level. Modules (`legacy-core-soap/`, `api-gateway/`, etc.) live directly at the repo root.

## 2026-08-06 — Git identity set locally, not globally

No global `user.name`/`user.email` was configured on this machine. Set `git config user.name "Robert Simonfi"` / `user.email "simonfi.robi@gmail.com"` scoped to this repo only, so it doesn't change behavior for any other repo on this machine.

## 2026-08-06 — Push workflow: commit locally, ask before every `git push`

Claude commits locally as modules are completed, but confirms with Robert before running `git push` each time, rather than pushing automatically or leaving all pushes to him manually.

## 2026-08-06 — Target Spring Boot 4.1.0, not Spring Boot 3

The original brief specified Spring Boot 3, but as of this build date Spring Initializr no longer offers any 3.x line — only 4.0.x/4.1.x (Spring Framework 7). Robert chose to target the current generation (4.1.0) rather than hand-pin an aged-out 3.x version, since the interview should reflect what's actually current. Watch for API differences from Boot 3 tutorials/docs (e.g. some Spring Security and WebFlux changes) while working through the modules.
