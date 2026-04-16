package com.sonyairplay.receiver

import android.media.AudioAttributes
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
    private var playingThread: Thread? = null

    fun playPcmFile(path: String) {
        stop()
        playingThread = thread {
            try {
                val sampleRate = 44100
                val channelConfig = AudioFormat.CHANNEL_OUT_STEREO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBuf = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val attr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val format = AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(attr)
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(minBuf * 4)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                audioTrack?.play()
                val fis = FileInputStream(File(path))
                val buffer = ByteArray(2048)
                var read = fis.read(buffer)
                while (read > 0) {
                    var offset = 0
                    while (offset < read) {
                        val wrote = audioTrack?.write(buffer, offset, read - offset) ?: 0
                        if (wrote > 0) offset += wrote else Thread.sleep(1)
                    }
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

    fun stop() {
        try {
            playingThread?.interrupt()
            playingThread = null
        } catch (e: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {}
        audioTrack = null
    }
}
