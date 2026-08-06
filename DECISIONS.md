# Decisions Log

Running record of the choices made while building this sandbox and why. Each entry is a decision point, not a changelog of every file — mechanical steps (adding a dependency, a getter, a config line) aren't logged here.

---

## 2026-08-06 — Repo layout: flat at repo root, not nested under `eidas-wallet-sandbox/`

The original project brief nests all modules under an `eidas-wallet-sandbox/` subfolder inside a monorepo. Since this repo (`eWalletING`) *is* the sandbox — there's no outer project it lives alongside — nesting would just add a redundant folder level. Modules (`legacy-core-soap/`, `api-gateway/`, etc.) live directly at the repo root.

## 2026-08-06 — Git identity set locally, not globally

No global `user.name`/`user.email` was configured on this machine. Set `git config user.name "Robert Simonfi"` / `user.email "simonfi.robi@gmail.com"` scoped to this repo only, so it doesn't change behavior for any other repo on this machine.

## 2026-08-06 — Push workflow: commit locally, ask before every `git push`

Claude commits locally as modules are completed, but confirms with Robert before running `git push` each time, rather than pushing automatically or leaving all pushes to him manually.
