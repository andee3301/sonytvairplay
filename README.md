# SonyTV AirPlay Receiver

SonyTV AirPlay Receiver is an experimental Android TV receiver for local AirPlay-style streaming. It is designed for personal use on your own hardware and focuses on pragmatic interoperability rather than full protocol coverage.

The app currently includes:
- AirPlay and RAOP service advertisement over mDNS
- Basic RTSP mirroring with H.264 decode through `MediaCodec`
- RAOP audio ingest with ALAC decode through FFmpegKit
- Lightweight control endpoints for pairing, play, and stop

## Status

This project builds cleanly with Gradle and GitHub Actions and can produce:
- a debug APK for sideload testing
- a signed release APK when signing credentials are provided

It does not implement FairPlay, AirPlay 2 DRM, or Apple certification features.

## Requirements

- Java 17
- Android SDK Platform 34
- Android Build-Tools 34.0.0
- Minimum Android version: API 24

## Local Build

Build both variants:

```bash
./gradlew clean assembleDebug assembleRelease
```

Install the debug APK:

```bash
adb connect <TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release output:
- unsigned: `app/build/outputs/apk/release/app-release-unsigned.apk`
- signed: `app/build/outputs/apk/release/app-release.apk`

The signed filename appears only when release signing is configured.

## Release Signing

Release signing can be supplied either through a local `keystore.properties` file or through GitHub Actions secrets.

An example file is included at `keystore.properties.example`.

Local `keystore.properties` format:

```properties
storeFile=signing/release-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

The following files are ignored by git:
- `keystore.properties`
- `signing/`

## GitHub Actions

The workflow lives at `.github/workflows/android-apk.yml`.

It runs on:
- pull requests
- pushes to `main` or `master`
- version tags such as `v1.0.1`
- manual dispatch

It uploads the generated APKs as workflow artifacts. On tag pushes it also creates a GitHub release automatically.

To enable signed release builds in GitHub Actions, add these repository secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

`ANDROID_KEYSTORE_BASE64` should be the base64-encoded contents of your `.jks` keystore file.

## Notes

- FFmpegKit adds substantial native payload size, so release APKs are relatively large.
- Native in-memory ALAC decode is not finished; the current path relies on FFmpegKit for compatibility.
- This repository is intended for local experimentation and sideload distribution, not store distribution.
