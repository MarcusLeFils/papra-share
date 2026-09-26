## [1.3.0] - 2026-09-26

### 🚀 Features

- Obtainium installation (deep link, badge, stable asset name) (#2)

### 🐛 Bug Fixes

- *(build)* Derive APK version from PAPRA_PACKAGE_VERSION env, dev fallback 0.0.1

### ⬆️ Dependency Updates

- *(deps)* Refresh flake.lock nixpkgs (16 Sep → 25 Sep)
- *(deps)* Bump actions/checkout from 4 to 7 (#3)
- *(deps)* Bump org.jetbrains.kotlinx:kotlinx-coroutines-android (#11)
- *(deps)* Bump softprops/action-gh-release from 2 to 3
- *(deps)* Bump DeterminateSystems/nix-installer-action from 4 to 23
- *(deps)* Bump kotlin from 2.2.21 to 2.4.20 (#6)
- *(deps)* Upgrade AGP 9.4.1 + Gradle 9.7.1 (drop kotlin.android plugin) (#13)

### ⚙️ Miscellaneous Tasks

- Add Dependabot for Gradle and GitHub Actions deps
- Inject release tag version into Gradle as PAPRA_PACKAGE_VERSION
## [1.1.0] - 2026-09-17

### 🚀 Features

- Internationalize the app (resource-based messages + English locale)

### 🐛 Bug Fixes

- *(i18n)* Move string mapping to UI, preserve failure cause on partial upload

### 📚 Documentation

- Add v1.1.0 changelog entry
## [1.0.0] - 2026-09-17

### 🚀 Features

- Declare system share intents for documents and scans
- Add UI resources (strings, material theme, launcher icon)
- Implement Papra upload client and settings persistence
- Resolve shared documents from system intents
- Add share and settings view models
- Build Compose UI (share and settings screens)
- Wire Main and Share activities

### 📚 Documentation

- Add project README (usage, config, dev environment)
- Add GenAI notice section to README

### ⚙️ Miscellaneous Tasks

- Bootstrap Android Gradle project and Nix dev environment
- Add MIT license and git-cliff changelog configuration