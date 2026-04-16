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

Limitations:
- Not Apple‑certified. FairPlay and AirPlay‑2 features not supported.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>