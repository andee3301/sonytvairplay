# Sony TV AirPlay Receiver (prototype)

This repository contains a prototype Android TV app that advertises an AirPlay-like
receiver via mDNS and exposes control endpoints. It's a starting point for building a
non-certified AirPlay receiver for personal use.

Phase 1 (MVP):
- mDNS advertise (_airplay._tcp)
- simple HTTP control endpoints (/play, /stop)
- RAOP audio receiver (ALAC decoding via FFmpeg-kit)
- Screen mirroring (RTSP/H.264)

Build & run (local dev):
- Open as Android project in Android Studio
- Build and generate APK (assembleDebug)
- Sideload the APK to your Android TV

Build & sideload (quick):

- Open the project in Android Studio (root folder). Let Android Studio download the Gradle wrapper and SDK.
- Build -> Build Bundle(s) / APK(s) -> Build APK(s).
- Sideload to your TV via adb:
  - adb connect <TV_IP>:5555
  - adb install -r app/build/outputs/apk/debug/app-debug.apk

FFmpeg usage & CI builds:
- This prototype integrates FFmpeg with the Android app via FFmpeg-kit to decode ALAC audio payloads captured from AirPlay (RAOP). The implementation decodes rotated ALAC chunks to raw PCM and streams to AudioTrack. This is a pragmatic prototype and may have latency or compatibility issues with some AirPlay variants.
- A GitHub Actions workflow (/.github/workflows/build_apk.yml) builds a debug APK and uploads it as an artifact for easy sideloading.

Limitations:
- Not Apple‑certified. FairPlay and AirPlay‑2 features are not supported.
- RAOP decryption and advanced control features are NOT implemented. This prototype attempts to decode ALAC but may not support encrypted or proprietary payloads.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>