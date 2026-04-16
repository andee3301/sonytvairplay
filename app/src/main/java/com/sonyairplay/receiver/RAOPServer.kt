package com.sonyairplay.receiver

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * RAOPServer (optimized)
 * - Reduced rotation interval and size threshold for low latency
 * - Single-threaded decode executor to avoid FFmpeg thrashing
 * - Integrates a simple RTCP manager that sends receiver reports periodically
 * - Streams decoded PCM chunks to AudioPlayer for playback
 */
object RAOPServer {
    private const val TAG = "RAOPServer"
    private var serverSocket: ServerSocket? = null
    private var running = false
    private var rtpSocket: DatagramSocket? = null
    private var currentOut: FileOutputStream? = null
    private var currentFile: File? = null
    private val lock = Object()
    private var decodeThreadRunning = false
    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private var rtcpManager: RtcpManager? = null

    // tuning parameters (aggressive, for low-latency personal use)
    private const val ROTATE_INTERVAL_MS = 150L
    private const val ROTATE_SIZE_THRESHOLD = 2048 // bytes
    private const val SERVER_RTP_PORT_DEFAULT = 6000

    fun start(context: Context, port: Int = 5000) {
        running = true
        thread {
            try {
                serverSocket = ServerSocket(port)
                Log.i(TAG, "RAOP RTSP server listening on $port")
                while (running) {
                    val client = serverSocket!!.accept()
                    Log.i(TAG, "RAOP client connected: ${'$'}{client.inetAddress}")
                    handleClient(context, client)
                }
            } catch (e: Exception) {
                Log.e(TAG, "RAOP server error", e)
            } finally {
                stop()
            }
        }
    }

    private fun handleClient(context: Context, socket: Socket) {
        thread {
            var clientRtpPort = -1
            var interleaved = false
            val clientAddr = socket.inetAddress
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = socket.getOutputStream()
                var line = reader.readLine() ?: return@thread
                val requestLine = line
                val method = requestLine.split(" ")[0]
                val headers = mutableMapOf<String, String>()
                var cseq: String? = null
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
                var sdp: String? = null
                if (contentLength > 0) {
                    val bodyChars = CharArray(contentLength)
                    reader.read(bodyChars, 0, contentLength)
                    sdp = String(bodyChars)
                }
                Log.i(TAG, "RAOP ${'$'}method headers: ${'$'}headers")
                if (method == "ANNOUNCE") {
                    // start a fresh stream file
                    rotateFile(context)
                    val resp = "RTSP/1.0 200 OK\r\nCSeq: ${'$'}cseq\r\nServer: SonyAirPlay/1.0\r\n\r\n"
                    writer.write(resp.toByteArray())
                    writer.flush()
                } else if (method == "SETUP") {
                    val transport = headers["transport"] ?: ""
                    val cpRegex = Regex("client_port=(\\d+)-(\\d+)")
                    val interRegex = Regex("interleaved=(\\d+)-(\\d+)")
                    cpRegex.find(transport)?.let { clientRtpPort = it.groupValues[1].toInt() }
                    if (interRegex.find(transport) != null) interleaved = true
                    val serverRtp = SERVER_RTP_PORT_DEFAULT
                    val respTransport = if (interleaved) "RTP/AVP/TCP;unicast;interleaved=0-1" else "RTP/AVP;unicast;client_port=${'$'}{clientRtpPort}-${'$'}{clientRtpPort+1};server_port=${'$'}{serverRtp}-${'$'}{serverRtp+1}"
                    val resp = "RTSP/1.0 200 OK\r\nCSeq: ${'$'}cseq\r\nTransport: ${'$'}respTransport\r\nSession: 12345678\r\n\r\n"
                    writer.write(resp.toByteArray())
                    writer.flush()
                } else if (method == "RECORD") {
                    val resp = "RTSP/1.0 200 OK\r\nCSeq: ${'$'}cseq\r\nSession: 12345678\r\n\r\n"
                    writer.write(resp.toByteArray())
                    writer.flush()
                    if (interleaved) {
                        readInterleaved(socket, context, clientAddr, clientRtpPort)
                    } else {
                        startRtpReceiver(context, SERVER_RTP_PORT_DEFAULT, clientAddr, clientRtpPort)
                    }
                } else {
                    val resp = "RTSP/1.0 200 OK\r\nCSeq: ${'$'}cseq\r\n\r\n"
                    writer.write(resp.toByteArray())
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "RAOP client handler error", e)
            } finally {
                try { socket.close() } catch (e: Exception) {}
            }
        }
    }

    private fun startRtpReceiver(context: Context, port: Int, clientAddr: java.net.InetAddress?, clientRtpPort: Int) {
        thread {
            try {
                rtpSocket = DatagramSocket(port)
                Log.i(TAG, "RAOP RTP receiving on ${'$'}port")
                // RTCP manager (send minimal receiver reports)
                if (clientAddr != null && clientRtpPort > 0) {
                    val clientRtcpPort = clientRtpPort + 1
                    rtcpManager = RtcpManager(clientAddr, clientRtcpPort)
                    rtcpManager?.start()
                }
                val buf = ByteArray(8192)
                val packet = DatagramPacket(buf, buf.size)
                while (running) {
                    rtpSocket?.receive(packet)
                    val data = packet.data
                    val len = packet.length
                    if (len <= 12) continue
                    val payload = data.copyOfRange(12, len)
                    appendPayload(context, payload)
                }
            } catch (e: Exception) {
                Log.e(TAG, "RAOP RTP error", e)
            } finally {
                try { rtpSocket?.close() } catch (e: Exception) {}
            }
        }
        startRotator(context)
    }

    private fun readInterleaved(socket: Socket, context: Context, clientAddr: java.net.InetAddress?, clientRtpPort: Int) {
        thread {
            try {
                val `in` = socket.getInputStream()
                while (running) {
                    // read $ (0x24)
                    val b = `in`.read()
                    if (b == -1) break
                    if (b != 0x24) continue
                    // channel
                    val channel = `in`.read()
                    val len1 = `in`.read()
                    val len2 = `in`.read()
                    if (len1 == -1 || len2 == -1) break
                    val len = (len1 shl 8) or len2
                    val payload = ByteArray(len)
                    var read = 0
                    while (read < len) {
                        val r = `in`.read(payload, read, len - read)
                        if (r == -1) break
                        read += r
                    }
                    if (payload.size > 12) {
                        val rtpPayload = payload.copyOfRange(12, payload.size)
                        appendPayload(context, rtpPayload)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Interleaved read error", e)
            }
        }
        if (clientAddr != null && clientRtpPort > 0) {
            val clientRtcpPort = clientRtpPort + 1
            rtcpManager = RtcpManager(clientAddr, clientRtcpPort)
            rtcpManager?.start()
        }
        startRotator(context)
    }

    private fun appendPayload(context: Context, payload: ByteArray) {
        synchronized(lock) {
            try {
                if (currentOut == null) {
                    currentFile = File(context.cacheDir, "raop_stream.alac")
                    currentOut = FileOutputStream(currentFile, true)
                }
                currentOut?.write(payload)
            } catch (e: Exception) {
                Log.e(TAG, "write payload error", e)
            }
        }
    }

    private fun startRotator(context: Context) {
        if (decodeThreadRunning) return
        decodeThreadRunning = true
        decodeExecutor.submit {
            try {
                while (running) {
                    Thread.sleep(ROTATE_INTERVAL_MS)
                    synchronized(lock) {
                        val cf = currentFile
                        if (cf != null && cf.exists() && cf.length() > ROTATE_SIZE_THRESHOLD) {
                            try {
                                currentOut?.flush()
                                currentOut?.close()
                                val rotated = File(context.cacheDir, "raop_chunk_${'$'}{System.currentTimeMillis()}.alac")
                                cf.renameTo(rotated)
                                currentFile = File(context.cacheDir, "raop_stream.alac")
                                currentOut = FileOutputStream(currentFile, true)
                                val pcmOut = File(context.cacheDir, "raop_chunk_${'$'}{System.currentTimeMillis()}.pcm")
                                // schedule decode task
                                decodeExecutor.submit {
                                    try {
                                        FFmpegAlacDecoder.decodeAlacToPcm(rotated.absolutePath, pcmOut.absolutePath) { ok, out ->
                                            if (ok) {
                                                AudioPlayer.playPcmFile(out)
                                                rotated.delete()
                                                File(out).delete()
                                            } else {
                                                Log.w(TAG, "Decoding failed for ${'$'}{rotated.absolutePath}")
                                            }
                                        }
                                    } catch (e: Exception) { Log.e(TAG, "decode task error", e) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "rotate error", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "rotator error", e)
            } finally {
                decodeThreadRunning = false
            }
        }
    }

    private fun rotateFile(context: Context) {
        synchronized(lock) {
            try { currentOut?.close() } catch (e: Exception) {}
            currentFile = File(context.cacheDir, "raop_stream.alac")
            currentOut = FileOutputStream(currentFile, true)
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) {}
        try { rtpSocket?.close() } catch (e: Exception) {}
        synchronized(lock) {
            try { currentOut?.close() } catch (e: Exception) {}
            currentOut = null
            currentFile = null
        }
        try { rtcpManager?.stop() } catch (e: Exception) {}
        decodeExecutor.shutdownNow()
    }
}
