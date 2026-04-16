package com.sonyairplay.receiver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import com.google.android.exoplayer2.ui.PlayerView

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
        RAOPServer.start(5000)
    }

    override fun onDestroy() {
        super.onDestroy()
        MdnsAdvertiser.stop()
        SimpleHttpServer.stop()
        RAOPServer.stop()
        PlayerManager.release()
    }
}
