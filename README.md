# SonyTV AirPlay Receiver (Prototype)

Personal, experimental AirPlay/RAOP receiver for Android TV (tested on Sony X85F, Android TV 9).

This project is intended for private use only. It is NOT Apple-certified and cannot support FairPlay or AirPlay2 DRM features.

Core features
- mDNS advertisement for `_airplay._tcp` and `_raop._tcp`
- RTSP mirroring (H.264 → MediaCodec decode)
- RAOP audio ingest with ALAC decoding (FFmpeg-kit fallback; native libavcodec via NDK optional)
- Basic PIN pairing and simple control endpoints (/play, /stop)

Quick start — sideload (recommended)
1. Use CI (recommended):
   - Push a tag `v1.0.0` or run the release workflow manually (Actions → Build and publish release APKs).
   - The workflow will build APKs and create a GitHub Release; APKs are attached as release assets.
2. Or build locally in Android Studio and install the debug APK via ADB:
   - `adb connect <TV_IP>:5555`
   - `adb install -r app/build/outputs/apk/debug/app-debug.apk`
3. Open the app on the TV. From an iPhone/macOS: select the device in AirPlay (mirroring or audio).

Native ALAC (NDK) — optional, low-latency path
- The project includes a JNI scaffold (app/src/main/cpp/native_alac.c) and CMake support that will link to libavcodec/libavformat/etc if the native libraries are present.
- CI can optionally download prebuilt ffmpeg-kit native libraries and place them under `app/src/main/cpp/libs/<abi>/lib*.so`. To enable this, set the repository secret `FFMPEG_KIT_DOWNLOAD_URL` to a direct URL to a ffmpeg-kit Android zip (prebuilt libs). The release CI step will extract and copy libavcodec/libavformat/libavutil/libswresample/libswscale into the NDK libs path before building.
- If native libs are not provided, the app falls back to the Java ffmpeg-kit decoder path (slightly higher latency).

How to enable native decode via CI
1. Add repo secret: `FFMPEG_KIT_DOWNLOAD_URL` → a direct downloadable zip of ffmpeg-kit with lib/<abi>/lib*.so files.
2. Push a release tag or dispatch the workflow; CI will copy native libs into `app/src/main/cpp/libs/*` and the CMakeLists will link them into the native_alac library.

Notes on latency and reliability
- Native libavcodec via NDK offers the best latency but increases CI complexity and APK size.
- The current RAOP pipeline includes aggressive buffering (rotate interval ≈150ms) and tuned AudioTrack buffer sizes. Tunables live in `RAOPServerOptimized.kt` and `AudioPlayer.kt`.

CI / Release pipeline
- `.github/workflows/release.yml` (this repo) builds APKs and creates a GitHub Release when a tag is pushed (or when run manually). APK artifacts are attached to the Release.
- Use the `FFMPEG_KIT_DOWNLOAD_URL` secret to enable native lib linking in CI.

Next steps planned
- Implement in-memory native ALAC decode using libavcodec (JNI) to replace file-rotate + ffmpeg fallback.
- Improve RTCP SR parsing and RTP→NTP mapping for tighter A/V sync.
- Optional: sign releases by adding a keystore and signing in CI (provide keystore via GitHub Secrets).

Caveats
- This is experimental reverse-engineered functionality. Use only for personal devices.
- FFmpeg-kit increases APK size significantly; plan for large artifacts when sideloading.

If you want, trigger a release now (provide a tag) or allow CI to fetch ffmpeg-kit native libs by setting `FFMPEG_KIT_DOWNLOAD_URL` secret.

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>
