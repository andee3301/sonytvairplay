package com.sonyairplay.receiver

import android.content.Context
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.MediaItem

object PlayerManager {
    private var player: SimpleExoPlayer? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        if (player == null) {
            player = SimpleExoPlayer.Builder(appContext!!).build()
        }
    }

    fun getPlayer(): SimpleExoPlayer? = player

    fun playUrl(url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    fun stop() {
        player?.stop()
    }

    fun release() {
        player?.release()
        player = null
    }
}
