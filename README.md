<p align="center">
<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2520%2522app.paprashare%2522%252C%2520%2522url%2522%253A%2520%2522https%253A%252F%252Fgithub.com%252FMarcusLeFils%252Fpapra-share%2522%252C%2520%2522author%2522%253A%2520%2522MarcusLeFils%2522%252C%2520%2522name%2522%253A%2520%2522papra-share%2522%252C%2520%2522preferredApkIndex%2522%253A%25200%252C%2520%2522additionalSettings%2522%253A%2520%2522%257B%257D%2522%252C%2520%2522overrideSource%2522%253A%2520null%257D"><img height="40" src=".github/assets/obtainium-badge.png" alt="Get it on Obtainium"></a>
</p>

# Papra Share

**Android application** built with **Jetpack Compose** that adds a system share target for documents and scans (PDF, ODT, DOC/DOCX, ODS, XLS/XLSX, PPT/PPTX, RTF, TXT, CSV, PNG, JPG, WEBP, HEIC, HEIF) and uploads them straight to your [Papra](https://papra.app) instance.

## How it works

- The app declares a **share activity** (`ShareActivity`) that catches `ACTION_SEND` / `ACTION_SEND_MULTIPLE` from the system, filtered on document/scan MIME types.
- From any file manager, pick **Share** → **Papra Share**; a Compose (material3) screen appears and the document(s) are uploaded.
- It calls `POST {instanceUrl}/api/organizations/{orgId}/documents` as multipart form-data (`file` field) with an `Authorization: Bearer <apiKey>` header.
- Required API key permission: **`documents:create`** (create it in your Papra user settings) — plus `organizations:read` if you want to list organizations.

### Configuration

Three fields, reachable from the launcher (tap **Papra Share** → *Papra Settings*):

| Field | Example |
|---|---|
| Instance URL | `https://api.papra.app` (or your self-hosted install, e.g. `https://papra.example.net`) |
| API key | token created under *User Settings → API keys* |
| Organization ID | ID of the destination organization |

Settings are persisted with `androidx.datastore:datastore-preferences`.

## Development environment

The environment is **declared in `flake.nix`** (Nix 2.34+). It provides:

- **JDK 17** (`openjdk`)
- **Android SDK**, platform **36** and build-tools **35.0.0** and **36.0.0** — built through nixpkgs `androidenv`
- **Gradle** 8 with the Gradle 8.14.3 wrapper
- `ANDROID_HOME` / `ANDROID_SDK_ROOT` / `JAVA_HOME` set automatically

```
nix develop

# then, inside the shell:
./gradlew assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

### Android license

The Android SDK is a **non-free** component; the flake accepts its license via
`config.android_sdk.accept_license = true;`. This acceptance is required for Nix evaluation.

## Project structure

```
papra-share/
├── flake.nix                     # dev environment (JDK, Android SDK, Gradle)
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml     # pinned versions (AGP, Kotlin, Compose, OkHttp…)
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml   # activities + share intent-filters
        ├── java/app/paprashare/
        │   ├── MainActivity.kt   # settings screen
        │   ├── ShareActivity.kt  # share screen
        │   ├── data/             # PapraApi (OkHttp) + SettingsRepository (DataStore)
        │   ├── ui/               # Compose screens + material3 theme
        │   └── util/             # received-document resolution
        └── res/                  # strings, themes, vector icons
```

## Tech stack

| Component | Version |
|---|---|
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 (+ Compose compiler) |
| Compose BOM | 2025.12.01 (material3, foundation, icons) |
| DataStore preferences | 1.1.7 |
| OkHttp | 4.12.0 |
| Target / compile SDK | 36 · minSdk 24 |

## Installation

Install and auto-update via **[Obtainium](https://obtainium.imranr.dev/)** (Android client for apps distributed as release artifacts):

- **One tap** — tap the badge at the top of this README (or use the [deep link](https://apps.obtainium.imranr.dev/redirect?r=obtainium%3A%2F%2Fapp%2F%257B%2522id%2522%253A%2520%2522app.paprashare%2522%252C%2520%2522url%2522%253A%2520%2522https%253A%252F%252Fgithub.com%252FMarcusLeFils%252Fpapra-share%2522%252C%2520%2522author%2522%253A%2520%2522MarcusLeFils%2522%252C%2520%2522name%2522%253A%2520%2522papra-share%2522%252C%2520%2522preferredApkIndex%2522%253A%25200%252C%2520%2522additionalSettings%2522%253A%2520%2522%257B%257D%2522%252C%2520%2522overrideSource%2522%253A%2520null%257D)).
- **Or manually** — in Obtainium: **Add App** → paste the repository URL `https://github.com/MarcusLeFils/papra-share` → pick **GitHub** and the latest release. Each release ships a single `papra-share-signed.apk`, so there is nothing else to configure.

Obtainium checks GitHub Releases periodically and can install/update in the background.

> **Note:** the APK is signed with a self-signed key (APK Signature Scheme v2). Enable *Settings → Install unknown sources* (or sideload) on your device the first time you install.

## Security note

The API key is stored locally in the app preferences and sent in the `Authorization` header. Prefer a TLS (HTTPS) endpoint on your self-hosted install, and scope the key to the `documents:create` permission only where possible, to limit the impact of an eventual leak.

## GenAI notice

This project is GenAI generated and maintained. [I](https://github.com/GaspardCulis) needed this app to simplify the upload process of documents on my phone to my Papra instance.
I couldn't bother making it myself, and saw the opportunity to do a little AI experiment, letting my _agent_ (I get cringed by this word) have it's own little project.
Usually not a fan of vibe coded projects, but it's a stoopidly simple app and it will let me see if Mr Marcus is capable of developing and maintaining this helpful little app and be of some use.
