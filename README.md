# Eruda DevTools

A mobile browser / debugger Android app written in Kotlin. Loads any URL in a WebView and automatically injects [Eruda](https://github.com/liriliri/eruda) — a full-featured mobile browser DevTools (console, network, elements, sources, resources, snippets, info panels).

## Features

- URL bar with Back / Forward / Refresh
- Full-screen WebView with JavaScript, DOM storage, mixed-content support
- Auto-injects Eruda after every page load
- Settings: toggle auto-inject, dark WebView background, default homepage
- Overflow menu: Inject Eruda Now, Reload, Copy URL, Open in Browser, Clear Cache, Settings
- Bottom status bar with current URL / loading state

## Build

```bash
chmod +x ./gradlew
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Adding the real Eruda

Replace `app/src/main/assets/eruda.js` with the actual eruda v3.4.3 file:

```bash
curl -L https://cdn.jsdelivr.net/npm/eruda@3.4.3/eruda.js -o app/src/main/assets/eruda.js
```

## CI

GitHub Actions builds a debug APK on every push to `main` and on pull requests, and uploads it as a build artifact (`eruda-devtools-debug-apk`).

## Tech

- Kotlin 1.9.23, AGP 8.3.2, Gradle 8.6
- minSdk 21, targetSdk 34, compileSdk 34
- XML layouts (no Jetpack Compose)
- Package: `com.ferdausfs.erudadevtools`
