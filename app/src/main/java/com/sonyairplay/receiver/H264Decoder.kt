package com.sonyairplay.receiver

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import kotlin.concurrent.thread
import java.nio.ByteBuffer

class H264Decoder(private val surface: Surface?) {
    private val TAG = "H264Decoder"
    private var codec: MediaCodec? = null
    @Volatile private var started = false

    fun start(sps: ByteArray?, pps: ByteArray?) {
        try {
            codec = MediaCodec.createDecoderByType("video/avc")
            val format = MediaFormat.createVideoFormat("video/avc", 1280, 720)
            if (sps != null) {
                try {
                    format.setByteBuffer("csd-0", ByteBuffer.wrap(sps))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set csd-0", e)
                }
            }
            if (pps != null) {
                try {
                    format.setByteBuffer("csd-1", ByteBuffer.wrap(pps))
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to set csd-1", e)
                }
            }
            codec?.configure(format, surface, null, 0)
            codec?.start()
            started = true
            thread {
                val info = MediaCodec.BufferInfo()
                while (started) {
                    try {
                        val outIndex = codec?.dequeueOutputBuffer(info, 10000) ?: -1
                        if (outIndex >= 0) {
                            codec?.releaseOutputBuffer(outIndex, true)
                        } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            Log.i(TAG, "Output format changed: ${'$'}{codec?.outputFormat}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Decoder output error", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start decoder", e)
            try { codec?.release() } catch (ex: Exception) {}
            codec = null
            started = false
        }
    }

    fun decode(nalWithStart: ByteArray, pts: Long) {
        if (codec == null) {
            start(null, null)
            if (codec == null) return
        }
        try {
            val inputIndex = codec?.dequeueInputBuffer(10000) ?: -1
            if (inputIndex >= 0) {
                val buffer = codec?.getInputBuffer(inputIndex)
                buffer?.clear()
                buffer?.put(nalWithStart)
                codec?.queueInputBuffer(inputIndex, 0, nalWithStart.size, pts, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decode error", e)
        }
    }

    fun stop() {
        started = false
        try { codec?.stop() } catch (e: Exception) {}
        try { codec?.release() } catch (e: Exception) {}
        codec = null
    }
}
