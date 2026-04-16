package com.sonyairplay.receiver

import android.util.Log
import android.view.Surface
import android.util.Base64
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import java.io.ByteArrayOutputStream

object MirroringServer {
    private const val TAG = "MirroringServer"
    private var serverSocket: ServerSocket? = null
    private var running = false
    private var rtpSocket: DatagramSocket? = null
    private var decoder: H264Decoder? = null
    private var serverRtpPort = 6000

    fun start(port: Int = 7001, surface: Surface) {
        running = true
        decoder = H264Decoder(surface)
        thread {
            try {
                serverSocket = ServerSocket(port)
                Log.i(TAG, "RTSP server listening on $port")
                while (running) {
                    val client = serverSocket!!.accept()
                    Log.i(TAG, "RTSP client connected: ${client.inetAddress}")
                    handleClient(client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "RTSP server error", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread {
            var sdp: String? = null
            var cseq: String? = null
            var clientRtpPort = -1
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream()
                var line = reader.readLine()
                if (line == null) return@thread
                val requestLine = line
                val method = requestLine.split(" ")[0]
                // read headers
                val headers = mutableMapOf<String, String>()
                while (true) {
                    line = reader.readLine() ?: break
                    if (line.trim().isEmpty()) break
                    val idx = line.indexOf(':')
                    if (idx > 0) {
                        val k = line.substring(0, idx).trim().lowercase()
                        val v = line.substring(idx + 1).trim()
                        headers[k] = v
                        if (k == "cseq") cseq = v
                    }
                }
                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                if (contentLength > 0) {
                    val bodyChars = CharArray(contentLength)
                    reader.read(bodyChars, 0, contentLength)
                    sdp = String(bodyChars)
                }
                Log.i(TAG, "RTSP $method headers: $headers")
                if (method == "ANNOUNCE" && sdp != null) {
                    // parse sprop-parameter-sets
                    val spropRegex = Regex("sprop-parameter-sets=([A-Za-z0-9+/=,]+)")
                    val match = spropRegex.find(sdp)
                    var sps: ByteArray? = null
                    var pps: ByteArray? = null
                    if (match != null) {
                        val parts = match.groupValues[1].split(",")
                        try {
                            sps = Base64.decode(parts[0], Base64.DEFAULT)
                            if (parts.size > 1) pps = Base64.decode(parts[1], Base64.DEFAULT)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to decode sprop", e)
                        }
                    }
                    decoder?.start(sps, pps)
                    val response = "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\nServer: SonyAirPlay/1.0\r\n\r\n"
                    writer.write(response.toByteArray())
                    writer.flush()
                    // read next requests
                    while (running) {
                        val reqLine = reader.readLine() ?: break
                        if (reqLine.trim().isEmpty()) continue
                        val reqMethod = reqLine.split(" ")[0]
                        val reqHeaders = mutableMapOf<String, String>()
                        var reqCseq: String? = null
                        while (true) {
                            val hdr = reader.readLine() ?: break
                            if (hdr.trim().isEmpty()) break
                            val idx2 = hdr.indexOf(':')
                            if (idx2 > 0) {
                                val k2 = hdr.substring(0, idx2).trim().lowercase()
                                val v2 = hdr.substring(idx2 + 1).trim()
                                reqHeaders[k2] = v2
                                if (k2 == "cseq") reqCseq = v2
                            }
                        }
                        Log.i(TAG, "RTSP $reqMethod headers: $reqHeaders")
                        if (reqMethod == "SETUP") {
                            val transport = reqHeaders["transport"] ?: ""
                            val cpRegex = Regex("client_port=(\\d+)-(\\d+)")
                            val cpMatch = cpRegex.find(transport)
                            if (cpMatch != null) {
                                clientRtpPort = cpMatch.groupValues[1].toInt()
                            }
                            val serverPort = serverRtpPort
                            val resp = "RTSP/1.0 200 OK\r\nCSeq: $reqCseq\r\nTransport: RTP/AVP;unicast;client_port=$clientRtpPort-${clientRtpPort + 1};server_port=$serverPort-${serverPort + 1}\r\nSession: 12345678\r\n\r\n"
                            writer.write(resp.toByteArray())
                            writer.flush()
                        } else if (reqMethod == "RECORD" || reqMethod == "PLAY") {
                            val resp = "RTSP/1.0 200 OK\r\nCSeq: $reqCseq\r\nSession: 12345678\r\n\r\n"
                            writer.write(resp.toByteArray())
                            writer.flush()
                            startRtpReceiver(serverRtpPort)
                        } else if (reqMethod == "TEARDOWN") {
                            val resp = "RTSP/1.0 200 OK\r\nCSeq: $reqCseq\r\nSession: 12345678\r\n\r\n"
                            writer.write(resp.toByteArray())
                            writer.flush()
                            break
                        } else {
                            val resp = "RTSP/1.0 200 OK\r\nCSeq: $reqCseq\r\n\r\n"
                            writer.write(resp.toByteArray())
                            writer.flush()
                        }
                    }
                } else {
                    val response = "RTSP/1.0 200 OK\r\nCSeq: $cseq\r\n\r\n"
                    writer.write(response.toByteArray())
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "RTSP handler error", e)
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private fun startRtpReceiver(port: Int) {
        thread {
            try {
                rtpSocket = DatagramSocket(port)
                Log.i(TAG, "RTP receiver listening on $port")
                val buf = ByteArray(4096)
                val packet = DatagramPacket(buf, buf.size)
                val baosLocal = ByteArrayOutputStream()
                while (running) {
                    rtpSocket?.receive(packet)
                    val data = packet.data
                    val len = packet.length
                    if (len < 12) continue
                    val payloadOffset = 12
                    val rtpTs = ((data[4].toLong() and 0xFF) shl 24) or ((data[5].toLong() and 0xFF) shl 16) or ((data[6].toLong() and 0xFF) shl 8) or (data[7].toLong() and 0xFF)
                    val payload = data.copyOfRange(payloadOffset, len)
                    if (payload.isEmpty()) continue
                    val nalType = payload[0].toInt() and 0x1F
                    if (nalType == 28) { // FU-A
                        val fuHeader = payload[1].toInt() and 0xFF
                        val start = (fuHeader and 0x80) != 0
                        val end = (fuHeader and 0x40) != 0
                        val reconstructedNal = ((payload[0].toInt() and 0xE0) or (fuHeader and 0x1F)).toByte()
                        if (start) {
                            baosLocal.reset()
                            baosLocal.write(byteArrayOf(0x00,0x00,0x00,0x01))
                            baosLocal.write(reconstructedNal.toInt())
                            baosLocal.write(payload, 2, payload.size - 2)
                        } else {
                            baosLocal.write(payload, 2, payload.size - 2)
                        }
                        if (end) {
                            val nal = baosLocal.toByteArray()
                            val pts = (rtpTs * 1000000L) / 90000L
                            decoder?.decode(nal, pts)
                            baosLocal.reset()
                        }
                    } else {
                        val nalWithStart = ByteArray(4 + payload.size)
                        nalWithStart[0] = 0x00
                        nalWithStart[1] = 0x00
                        nalWithStart[2] = 0x00
                        nalWithStart[3] = 0x01
                        System.arraycopy(payload, 0, nalWithStart, 4, payload.size)
                        val pts = (rtpTs * 1000000L) / 90000L
                        decoder?.decode(nalWithStart, pts)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "RTP receiver error", e)
            } finally {
                try { rtpSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) {}
        try { rtpSocket?.close() } catch (e: Exception) {}
        try { decoder?.stop() } catch (e: Exception) {}
    }
}
