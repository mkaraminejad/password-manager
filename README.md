# SecureVault - Offline-First Secure Android Password Manager

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue.svg)](https://kotlinlang.org)
[![MinSDK](https://img.shields.io/badge/MinSDK-26-orange.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-green.svg)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Database-SQLCipher%20AES--256-red.svg)](https://www.zetetic.net/sqlcipher/)

> ⚠️ **SECURITY WARNING & DISCLAIMER**  
> This application is an educational, offline-first personal reference implementation. Before utilizing this application to store valuable, high-stakes real-world production credentials, it should undergo an independent, formal cryptographic and application-security review.

---

## Complete Android Project & Interactive Companion
This repository contains:
1. **Complete Android Studio Project** in the `/android` directory (Gradle scripts, Kotlin Clean Architecture MVVM code, Room + SQLCipher database, Android Keystore encryption wrapper, Biometric unlock, Autofill service, unit tests, and UI tests).
2. **Interactive Web Companion & Simulator** in the root `/src` directory (interactive Material 3 Phone Simulator, code viewer, unit test runner, and one-click Android Studio Project ZIP exporter).

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

## 2. Threat Model & Security Decisions

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

## 3. Cryptographic Design

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

## 4. Building & Running in Android Studio

1. Open Android Studio (Hedgehog or newer).
2. Open the `android/` directory.
3. Allow Gradle to synchronize dependencies.
4. Run on an emulator or physical device running Android 8.0 (API 26) or newer.

Run unit tests via:
```bash
./gradlew testDebugUnitTest
```

Run UI tests via:
```bash
./gradlew connectedAndroidTest
```
