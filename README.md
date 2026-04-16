# SonyTV AirPlay Receiver (Prototype)

Personal, experimental AirPlay/RAOP receiver for Android TV (tested on Sony X85F, Android TV 9).

This project is intended for private use only. It is NOT Apple-certified and cannot support FairPlay or AirPlay2 DRM features.

Core features
- mDNS advertisement for `_airplay._tcp` and `_raop._tcp`
- RTSP mirroring (H.264 → MediaCodec decode)
- RAOP audio ingest with ALAC decoding via FFmpegKit
- Basic PIN pairing and simple control endpoints (/play, /stop)

Build requirements
- Android SDK platform 34 / build-tools 34.0.0
- Java 17
- Minimum supported Android version: API 24

Local build
1. Run `./gradlew clean assembleDebug assembleRelease`
2. Install the debug APK:
   - `adb connect <TV_IP>:5555`
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions
- Workflow: `.github/workflows/android-apk.yml`
- Triggers on pull requests, pushes to `main`/`master`, version tags like `v1.0.0`, and manual dispatch
- Uploads:
  - `app/build/outputs/apk/debug/app-debug.apk`
  - `app/build/outputs/apk/release/app-release-unsigned.apk`

Dependency note
- The original `com.arthenica` FFmpegKit Android artifacts were retired and removed. This project now uses the maintained `com.moizhassan.ffmpeg:ffmpeg-kit-16kb:6.1.1` fork so fresh CI builds continue to resolve and compile.

Notes on latency and reliability
- The current RAOP pipeline includes aggressive buffering (rotate interval ≈150ms) and tuned AudioTrack buffer sizes. Tunables live in `RAOPServerOptimized.kt` and `AudioPlayer.kt`.

Next steps planned
- Improve RTCP SR parsing and RTP→NTP mapping for tighter A/V sync.
- Optional: sign release builds in CI by adding a keystore and signing config via GitHub Secrets.

Caveats
- This is experimental reverse-engineered functionality. Use only for personal devices.
- FFmpeg-kit increases APK size significantly; plan for large artifacts when sideloading.
