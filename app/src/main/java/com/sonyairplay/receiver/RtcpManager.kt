package com.sonyairplay.receiver

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

/**
 * Minimal RTCP manager: sends Receiver Reports periodically to the client
 * This is a pragmatic implementation for improved interoperability and sync
 */
class RtcpManager(private val peerAddress: InetAddress, private val peerPort: Int) {
    private var running = false
    private var socket: DatagramSocket? = null
    private val TAG = "RtcpManager"
    private val ssrc = (System.currentTimeMillis() and 0xffffffffL).toInt()

    fun start() {
        running = true
        thread {
            try {
                socket = DatagramSocket()
                // send an initial RR immediately
                while (running) {
                    sendReceiverReport()
                    Thread.sleep(5000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "rtcp error", e)
            } finally {
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }

    private fun sendReceiverReport() {
        try {
            // RTCP RR header: V=2 P=0 RC=0 (0x81), PT=201 (RR), length=1 (32-bit words - 1)
            val buf = ByteArray(8)
            buf[0] = 0x81.toByte()
            buf[1] = 201.toByte()
            buf[2] = 0x00.toByte()
            buf[3] = 0x01.toByte()
            // sender SSRC
            val ssrcVal = ssrc
            buf[4] = ((ssrcVal shr 24) and 0xFF).toByte()
            buf[5] = ((ssrcVal shr 16) and 0xFF).toByte()
            buf[6] = ((ssrcVal shr 8) and 0xFF).toByte()
            buf[7] = (ssrcVal and 0xFF).toByte()
            val packet = DatagramPacket(buf, buf.size, peerAddress, peerPort)
            socket?.send(packet)
        } catch (e: Exception) {
            Log.e(TAG, "send rr failed", e)
        }
    }

    fun stop() {
        running = false
        try { socket?.close() } catch (e: Exception) {}
    }
}
