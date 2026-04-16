package com.sonyairplay.receiver

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

object RAOPServer {
    private const val TAG = "RAOPServer"
    private var socket: DatagramSocket? = null
    private var running = false

    fun start(port: Int = 5000) {
        running = true
        thread {
            try {
                socket = DatagramSocket(port)
                val buf = ByteArray(4096)
                val packet = DatagramPacket(buf, buf.size)
                Log.i(TAG, "RAOP UDP server listening on $port")
                while (running) {
                    socket?.receive(packet)
                    Log.d(TAG, "Received RAOP packet from ${packet.address}:${packet.port} size=${packet.length}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "RAOP server error", e)
            } finally {
                socket?.close()
            }
        }
    }

    fun stop() {
        running = false
        socket?.close()
    }
}
