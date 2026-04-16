package com.sonyairplay.receiver

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import java.io.File
import java.io.FileInputStream
import kotlin.concurrent.thread

object AudioPlayer {
    private const val TAG = "AudioPlayer"
    private var audioTrack: AudioTrack? = null

    fun playPcmFile(path: String) {
        thread {
            try {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig, audioFormat, minBuf, AudioTrack.MODE_STREAM)
                audioTrack?.play()
                val fis = FileInputStream(File(path))
                val buffer = ByteArray(4096)
                var read = fis.read(buffer)
                while (read > 0) {
                    audioTrack?.write(buffer, 0, read)
                    read = fis.read(buffer)
                }
                fis.close()
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (e: Exception) {
                Log.e(TAG, "play error", e)
            }
        }
    }
}
