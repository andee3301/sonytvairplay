# Sony TV AirPlay Receiver (prototype)

This repository contains a prototype Android TV app that advertises an AirPlay-like
receiver via mDNS and exposes control endpoints. It's a starting point for building a
non-certified AirPlay receiver for personal use.

Phase 1 (MVP):
- mDNS advertise (_airplay._tcp)
- simple HTTP control endpoints (/play)
- RAOP audio receiver (future)
- mirroring support (future)

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

Notes:
- RAOP (AirPlay audio) is implemented as a UDP listener stub only; ALAC decoding / RAOP encryption is not implemented in this prototype. ExoPlayer handles most media URL playback and HLS.
- This is for personal use only; not Apple‑certified. Some AirPlay features (FairPlay DRM, AirPlay 2 multiroom) will not work.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>