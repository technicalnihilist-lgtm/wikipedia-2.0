# Wikipedia 2.0 — desktop

Electron wrapper around the same `index.html` the web app uses. One codebase, three installers.

## Run locally

```bash
cd desktop
npm install
npm start
```

## Build installers

```bash
npm run dist          # for the OS you are on
npm run dist:win      # .exe (NSIS installer + portable)
npm run dist:mac      # .dmg + .zip
npm run dist:linux    # .AppImage + .deb
```

Output lands in `desktop/dist/`.

## Or let GitHub build them

`.github/workflows/desktop.yml` builds all three on every tag — Windows, macOS and Linux runners
each produce their own installers and attach them to the release. No local toolchain needed.

## Notes

- The window loads `index.html` from the repo root (copied next to `main.js` at build time).
- Any link that leaves the app opens in the system browser.
- Everything is fetched live from the Wikimedia APIs — the desktop app stores nothing either.
