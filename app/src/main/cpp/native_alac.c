#include <jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>

#define LOG_TAG "native_alac"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

JNIEXPORT jboolean JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeInit(JNIEnv* env, jobject thiz) {
    LOGI("nativeInit called (stub)");
    return JNI_FALSE; // stub: native ALAC decode not implemented
}

JNIEXPORT jbyteArray JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeDecode(JNIEnv* env, jobject thiz, jbyteArray input) {
    LOGI("nativeDecode called (stub)");
    // Not implemented: return null to indicate fallback should be used
    return NULL;
}

JNIEXPORT void JNICALL Java_com_sonyairplay_receiver_NativeAlac_nativeRelease(JNIEnv* env, jobject thiz) {
    LOGI("nativeRelease called (stub)");
}
