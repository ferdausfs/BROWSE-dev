# Eruda DevTools

Android browser app with built-in [Eruda](https://github.com/liriliri/eruda) mobile DevTools.

## Features
- Full WebView browser with URL bar, back/forward/refresh
- Auto-injects Eruda on every page load
- Console, Network, Elements, Sources, Resources, Snippets, Info panels
- Settings: toggle auto-inject, set homepage
- Manual "Inject Eruda" menu option

## Setup

### 1. Add eruda.js
Copy the built `eruda.js` into `app/src/main/assets/eruda.js`.

Get it from the [eruda releases](https://github.com/liriliri/eruda/releases) or use the CDN version:
```
https://cdn.jsdelivr.net/npm/eruda@3.4.3/eruda.min.js
```

### 2. Build
```bash
./gradlew assembleDebug
```
APK will be at `app/build/outputs/apk/debug/`.

## CI
GitHub Actions auto-builds on every push to `main`. Download APK from the Actions artifacts tab.
