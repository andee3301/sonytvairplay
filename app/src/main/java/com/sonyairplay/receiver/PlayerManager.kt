package com.sonyairplay.receiver

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

object PlayerManager {
    private var player: ExoPlayer? = null

    fun init(context: Context) {
        if (player == null) {
            player = ExoPlayer.Builder(context.applicationContext).build()
        }
    }

    fun getPlayer(): ExoPlayer? = player

    fun playUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
    }

    fun release() {
        player?.release()
        player = null
    }
}
