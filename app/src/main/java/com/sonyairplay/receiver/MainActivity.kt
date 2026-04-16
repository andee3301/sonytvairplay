package com.sonyairplay.receiver

import android.os.Bundle
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView

class MainActivity : AppCompatActivity() {
    private var playerView: PlayerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.label).text = "SonyTV AirPlay Receiver (Prototype)"

        // init components
        PlayerManager.init(this)
        PairingManager.init(this)
        playerView = findViewById(R.id.player_view)
        playerView?.player = PlayerManager.getPlayer()

        // start services
        MdnsAdvertiser.start("SonyTV-AirPlay-Proto")
        SimpleHttpServer.start(7000)
        // start optimized RAOP server (native decode preferred)
        RAOPServerOptimized.start(this, 5000)

        // mirroring surface - start RTSP server when surface is ready
        val surfaceView = findViewById<SurfaceView>(R.id.mirroring_surface)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val surface: Surface = holder.surface
                MirroringServer.start(7001, surface)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                MirroringServer.stop()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        MdnsAdvertiser.stop()
        SimpleHttpServer.stop()
        RAOPServerOptimized.stop()
        MirroringServer.stop()
        PlayerManager.release()
    }
}
