# SonyTV AirPlay Receiver (Prototype)

An experimental, personal-use AirPlay/RAOP receiver for Android TV (Sony X85F tested).
This project aims to provide a practical, non-certified AirPlay receiver supporting:

- mDNS advertisement for AirPlay and RAOP
- RTSP mirroring (H.264 → MediaCodec decode)
- RAOP (AirPlay audio) with ALAC decoding via FFmpeg-kit and low-latency playback
- Basic pairing (PIN) and simple control endpoints (/play, /stop)

Important: This is a reverse-engineered, personal project. It is NOT Apple-certified and will not support FairPlay DRM or all AirPlay2 features.

Quick start (sideload)
1. Build debug APK (locally or use the provided GitHub Actions):
   - Local: open in Android Studio, Build -> Build APK(s)
   - CI: push to `main`; workflow will build and attach app-debug.apk as an artifact
2. Sideload via ADB:
   - `adb connect <TV_IP>:5555`
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Launch app on TV. On a source device (iPhone/macOS) the receiver should appear as an AirPlay target:
   - AirPlay for screen mirroring: select the target (RTSP mirroring implemented)
   - AirPlay audio: select the target for audio (RAOP)

What's optimized in this repo
- RAOP pipeline:
  - Small buffer rotation (150ms / ~2KB chunks) for aggressive latency reduction
  - Single-thread FFmpeg decode queue to avoid CPU thrash
  - Streaming playback via AudioTrack with reduced buffers
  - Minimal RTCP Receiver Reports to improve sender behaviour and sync
- Mirroring:
  - RTSP ANNOUNCE/SETUP/RECORD flow and RTP FU-A reassembly
  - MediaCodec decode to Surface for low-overhead rendering

CI / Build pipeline
- `.github/workflows/build_pipeline.yml` builds debug and release unsigned APKs and uploads them as artifacts for easy sideloading.

Limitations & Next steps
- Encrypted RAOP (FairPlay) and AirPlay2 multiroom are not supported.
- This prototype uses FFmpeg-kit for ALAC decoding; for the lowest possible latency consider a native ALAC decoder integrated via NDK.
- Further sync improvements (accurate RTCP SR parsing, RTP timestamp to NTP mapping) can reduce AV skew.

Security & legal
- Do not distribute this app commercially. Reverse-engineering and using Apple protocols beyond personal experimentation may have legal implications.

Contributing
- This is a personal project scaffold. PRs and improvements welcome — start by running the CI to validate build artifacts.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
