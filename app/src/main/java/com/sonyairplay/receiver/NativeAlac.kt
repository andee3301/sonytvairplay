package com.sonyairplay.receiver

object NativeAlac {
    private var available = false

    init {
        try {
            System.loadLibrary("native_alac")
            available = true
        } catch (t: Throwable) {
            available = false
        }
    }

    private external fun nativeInit(): Boolean
    private external fun nativeDecode(input: ByteArray): ByteArray?
    private external fun nativeRelease()

    fun init(): Boolean {
        return try {
            if (!available) return false
            nativeInit()
        } catch (t: Throwable) {
            available = false
            false
        }
    }

    fun decode(input: ByteArray): ByteArray? {
        if (!available) return null
        return try {
            nativeDecode(input)
        } catch (t: Throwable) {
            available = false
            null
        }
    }

    fun release() {
        try {
            if (available) nativeRelease()
        } catch (t: Throwable) {
        }
    }

    fun isAvailable(): Boolean = available
}
