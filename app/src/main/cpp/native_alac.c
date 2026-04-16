#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>

#define LOG_TAG "native_alac"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Runtime handles for optional ffmpeg native libs (libavcodec, libavutil, libswresample)
static void* handle_avcodec = NULL;
static void* handle_avutil = NULL;
static void* handle_swr = NULL;
static int native_available = 0;

static int try_load_ffmpeg_libs() {
    // Try common library names; CI will place prebuilt libs under app/src/main/cpp/libs/<abi>/lib*.so
    if (handle_avcodec) return 1;

    handle_avcodec = dlopen("libavcodec.so", RTLD_NOW);
    if (!handle_avcodec) {
        LOGI("libavcodec not found via dlopen: %s", dlerror());
        return 0;
    }

    handle_avutil = dlopen("libavutil.so", RTLD_NOW);
    if (!handle_avutil) {
        LOGI("libavutil not found via dlopen: %s", dlerror());
        // continue; some decoders may still work, but warn
    }

    handle_swr = dlopen("libswresample.so", RTLD_NOW);
    if (!handle_swr) {
        LOGI("libswresample not found via dlopen: %s", dlerror());
    }

    LOGI("Found ffmpeg native libs (at least libavcodec)");
    return 1;
}

JNIEXPORT jboolean JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeInit(JNIEnv* env, jobject thiz) {
    LOGI("nativeInit called — attempting to load ffmpeg libs");
    if (try_load_ffmpeg_libs()) {
        native_available = 1;
        return JNI_TRUE;
    }
    native_available = 0;
    return JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeDecode(JNIEnv* env, jobject thiz, jbyteArray input) {
    LOGI("nativeDecode called — native_available=%d", native_available);
    if (!native_available) return NULL;

    // NOTE: Full in-memory libavcodec decoding implementation is non-trivial and depends
    // on the exact ALAC framing being fed. This function is a placeholder that confirms
    // native runtime libraries are present. A complete implementation will:
    //  - initialize AVCodec/AVCodecContext for ALAC
    //  - feed packets (from input byte array) into decoder via avcodec_send_packet
    //  - receive AVFrame and use libswresample to convert to s16le PCM
    //  - return PCM bytes as a Java byte[]
    // For now, return NULL to allow Java/FFmpegKit fallback.

    LOGI("nativeDecode: runtime libs present but decoding not yet implemented");
    return NULL;
}

JNIEXPORT void JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeRelease(JNIEnv* env, jobject thiz) {
    LOGI("nativeRelease called — cleaning up");
    if (handle_swr) { dlclose(handle_swr); handle_swr = NULL; }
    if (handle_avutil) { dlclose(handle_avutil); handle_avutil = NULL; }
    if (handle_avcodec) { dlclose(handle_avcodec); handle_avcodec = NULL; }
    native_available = 0;
}
