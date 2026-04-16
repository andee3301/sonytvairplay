package com.sonyairplay.receiver

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.view.View

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.label).text = "SonyTV AirPlay Receiver (Prototype)"

        // start mDNS and HTTP server
        MdnsAdvertiser.start("SonyTV-AirPlay-Proto")
        SimpleHttpServer.start(7000)
    }

    override fun onDestroy() {
        super.onDestroy()
        MdnsAdvertiser.stop()
        SimpleHttpServer.stop()
    }
}
