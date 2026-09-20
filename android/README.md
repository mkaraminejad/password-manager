# SecureVault - Offline-First Secure Android Password Manager

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue.svg)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/MinSDK-26-orange.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Database-SQLCipher%20AES--256-red.svg)](https://www.zetetic.net/sqlcipher/)

> ⚠️ **SECURITY WARNING & DISCLAIMER**  
> This application is an educational, offline-first personal reference implementation. Before utilizing this application to store valuable, high-stakes real-world production credentials, it should undergo an independent, formal cryptographic and application-security review.

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Threat Model & Security Decisions](#threat-model--security-decisions)
4. [Cryptographic Design](#cryptographic-design)
5. [Core Features](#core-features)
6. [Project Structure](#project-structure)
7. [Building & Running the App](#building--running-the-app)
8. [Testing Strategy](#testing-strategy)
9. [Limitations & Assumptions](#limitations--assumptions)

---

## 1. Overview
SecureVault is an offline-first personal password manager for Android (Min SDK 26) built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Clean Architecture (MVVM)**.

It enforces zero-network trust:
- **No Internet Permission**: The app does not declare `android.permission.INTERNET` in `AndroidManifest.xml`.
- **Zero Cloud Sync**: No external cloud backends, tracking, analytics, crash telemetry, or third-party ads.
- **Hardware-Protected Keys**: Database keys are wrapped using authenticated AES-GCM via the Android Keystore (backed by TEE / StrongBox where hardware permits).
- **Encrypted Database at Rest**: Powered by SQLCipher AES-256 encrypted SQLite Room database.
- **Strict In-Memory Zeroing**: Master password `CharArray` instances and transient byte keys are explicitly wiped with zeros (`\u0000` / `0x00`) immediately after derivation and upon lock.

---

## 2. Architecture

The codebase strictly follows **Clean Architecture** separated into distinct layers:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│  (Jetpack Compose + Material 3 + ViewModels + Flows)   │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                      Domain Layer                      │
│     (VaultEntry, PasswordStrength, VaultRepository)    │
└──────────────────────────┬─────────────────────────────┘
                           │
┌──────────────────────────▼─────────────────────────────┐
│                       Data Layer                       │
│  - Local: Room Database with SQLCipher SupportFactory   │
│  - Preferences: Hardware-EncryptedSharedPreferences   │
│  - Security Engine: CryptoManager & KeyDerivation      │
└────────────────────────────────────────────────────────┘
```

---

## 3. Threat Model & Security Decisions

| Threat / Attack Vector | Mitigation in SecureVault |
|---|---|
| **Physical Theft of Device (Powered Off)** | Entire SQLite database file encrypted via SQLCipher AES-256. Database encryption key is derived via PBKDF2 (120,000 rounds) from master password; never stored in plaintext on flash storage. |
| **Physical Theft of Device (Unlocked)** | Auto-lock idle timer locks the vault after 1, 5, 15, or 30 minutes of inactivity, or immediately upon app backgrounding. BiometricPrompt or master password required to unlock. |
| **Shoulder Surfing / Screen Sniffers** | Passwords masked by default with bullet characters (`••••••••`). Explicit unmask button. `FLAG_SECURE` prevents system screenshots and obscures app preview in Android Recents / App Switcher. |
| **Malicious Background Apps Reading Clipboard** | Android 13+ `ClipDescription.EXTRA_IS_SENSITIVE = true` hides clipboard overlays. Automated background coroutine timer purges copied credentials after configurable interval (default 30s). |
| **Memory Dump / Cold-Boot Extraction** | Master password handled exclusively as `CharArray` and zeroed out immediately after derivation. Transient vault key in `VaultKeyHolder` zeroized upon lock. |
| **Exfiltration via Malicious Network Calls** | Android Manifest declares zero network permissions (`android.permission.INTERNET` omitted completely). |
| **Malicious Cloud Backups** | `android:allowBackup="false"` in `AndroidManifest.xml` stops Android Auto-Backup or ADB backups from extracting unencrypted application data. |
| **Brute-Force Master Password Attacks** | PBKDF2-HMAC-SHA256 with 120,000 iterations + 32-byte cryptographically secure random salt. Software-enforced progressive delay after repeated invalid unlock attempts. |

---

## 4. Cryptographic Design

1. **Key Derivation (KDF)**:
   - Algorithm: `PBKDF2WithHmacSHA256`
   - Iteration Count: `120,000` (balanced for resistance vs. mobile UI thread latency)
   - Salt: 32 bytes (256 bits) sourced from `java.security.SecureRandom`
   - Key Length: 256 bits (32 bytes)
2. **Database At Rest**:
   - Engine: SQLCipher 4.5.4
   - Cipher: 256-bit AES in CBC mode with HMAC-SHA512 page integrity checking
3. **Master Key Hardware Protection**:
   - Keystore Alias: `SecureVaultDatabaseMasterKey`
   - Algorithm: AES-256-GCM (`AES/GCM/NoPadding`)
4. **Encrypted Backup Container**:
   - Binary format: `[SVLT Magic 4B][Version 2B][Iterations 4B][Salt 32B][IV 12B][AES-256-GCM Tagged Ciphertext]`
   - Authenticated Encryption: Any file tampering or incorrect password throws `AEADBadTagException` prior to parsing.

---

## 5. Core Features

1. **First-Run Setup**: Enforces 12+ character master password with real-time entropy calculation.
2. **Vault Management**: Add, edit, duplicate, favorite, delete, and search across titles, usernames, URLs, and notes.
3. **Password Generator**:
   - Mode 1: Random characters (8–48 chars, uppercase, lowercase, numbers, symbols, ambiguous character exclusion).
   - Mode 2: Passphrase (3–8 Diceware-inspired words, custom delimiters, capitalization, and numbers).
4. **Security Audit**: Analyzes vault for weak entropy (<12 chars or dictionary patterns) and cross-account password reuse.
5. **Autofill Service**: Clean Android Autofill service (`VaultAutofillService`) that requires an unlocked vault before serving credentials.
6. **Encrypted Export & Import**: Export credentials encrypted with an independent backup passphrase.

---

## 6. Project Structure

```
android/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/ (values, xml/autofill_service_config.xml)
        │   └── java/com/securevault/passwordmanager/
        │       ├── PasswordManagerApp.kt
        │       ├── MainActivity.kt
        │       ├── core/
        │       │   ├── model/ (VaultEntry, SecurityAudit)
        │       │   ├── security/ (CryptoManager, KeyDerivation, VaultKeyHolder, SecureClipboardManager, PasswordGenerator, PasswordStrengthAnalyzer, SecureBackupManager)
        │       │   ├── autofill/ (VaultAutofillService)
        │       │   └── timeout/ (IdleTimeoutTracker)
        │       ├── data/
        │       │   ├── local/ (VaultDatabase, VaultDao, VaultEntity)
        │       │   ├── preferences/ (AppPreferences)
        │       │   └── repository/ (VaultRepositoryImpl)
        │       ├── domain/repository/ (VaultRepository)
        │       └── ui/
        │           ├── theme/ (Color, Theme, Type)
        │           ├── navigation/ (Screen, AppNavGraph)
        │           ├── setup/ (SetupScreen, SetupViewModel)
        │           ├── unlock/ (UnlockScreen, UnlockViewModel)
        │           ├── vault/ (VaultScreen, VaultViewModel)
        │           ├── entry/ (EntryDetailEditScreen, EntryViewModel)
        │           ├── generator/ (PasswordGeneratorSheet)
        │           ├── audit/ (SecurityAuditScreen, SecurityAuditViewModel)
        │           └── settings/ (SettingsScreen, SettingsViewModel)
        ├── test/java/com/securevault/passwordmanager/ (6 Unit Test Suites)
        └── androidTest/java/com/securevault/passwordmanager/ (5 UI Test Suites)
```

---

## 7. Building & Running the App

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- Device or Emulator running Android 8.0 (API 26) or newer

### Steps
1. Open Android Studio.
2. Select **File > Open** and choose the `android/` directory.
3. Allow Gradle to synchronize dependencies.
4. Select `app` run configuration and click **Run** (or `Shift + F10`).

---

## 8. Testing Strategy

### Unit Tests (`app/src/test/`)
Run in Android Studio via `./gradlew testDebugUnitTest`:
- `PasswordGeneratorTest`: Validates length constraints, character set inclusion, ambiguous filtering, and passphrase formatting.
- `KeyDerivationTest`: Verifies PBKDF2 determinism, salt divergence, and memory zeroing (`wipeChars`, `wipeBytes`).
- `CryptoManagerTest`: Tests AES-256-GCM encryption/decryption roundtrip and verifies `AEADBadTagException` on ciphertext tampering.
- `VaultRepositoryTest`: Verifies CRUD, filtering, and duplication logic.
- `IdleTimeoutTrackerTest`: Verifies idle timer counting, unlock triggers, and key zeroization upon lock.
- `PasswordStrengthAnalyzerTest`: Verifies Shannon entropy checks and cross-vault password reuse detection.

### UI / Instrumentation Tests (`app/src/androidTest/`)
Run on a connected device/emulator via `./gradlew connectedAndroidTest`:
- `SetupFlowUiTest`: Tests master password length validation and initialization flow.
- `UnlockFlowUiTest`: Verifies unlock screen biometric and password UI components.
- `VaultEntryUiTest`: Tests creating, validating, and saving credentials.
- `SearchAndFilterUiTest`: Tests real-time searching across vault entries.
- `LockTimeoutUiTest`: Tests manual vault locking and immediate session termination.

---

## 9. Limitations & Assumptions
1. **OS Integrity Assumption**: The threat model assumes the Android OS is not rooted and that the Linux kernel / Android sandboxing is not compromised. On a compromised/rooted OS, root-level memory inspection can extract keys from RAM.
2. **Device Hardware Keystore**: StrongBox or TEE hardware key backing depends on device manufacturer support. On older devices lacking hardware Keystores, Android fallback software keys are used.
3. **Local-Only Constraints**: No multi-device sync is provided by design. Users must manually create and transfer encrypted `.svlt` backups to migrate credentials.
