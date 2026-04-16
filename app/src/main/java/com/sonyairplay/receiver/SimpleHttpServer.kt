package com.sonyairplay.receiver

import fi.iki.elonen.NanoHTTPD
import android.util.Log

object SimpleHttpServer {
    private const val TAG = "SimpleHttpServer"
    private var server: NanoHTTPD? = null

    fun start(port: Int = 7000) {
        server = object : NanoHTTPD(port) {
            override fun serve(session: IHTTPSession): Response {
                val uri = session.uri
                val method = session.method
                val headers = session.headers
                val files = HashMap<String, String>()
                if (method == Method.POST) {
                    try {
                        session.parseBody(files)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
                return when (uri) {
                    "/" -> newFixedLengthResponse(Response.Status.OK, "text/plain", "SonyTV AirPlay Receiver")
                    "/play" -> {
                        val location = headers["content-location"] ?: files["postData"] ?: session.parameters["url"]?.firstOrNull()
                        if (!location.isNullOrEmpty()) {
                            Log.i(TAG, "Play request: $location")
                            PlayerManager.playUrl(location)
                            newFixedLengthResponse(Response.Status.OK, "text/plain", "OK")
                        } else {
                            newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing URL")
                        }
                    }
                    "/stop" -> {
                        PlayerManager.stop()
                        newFixedLengthResponse(Response.Status.OK, "text/plain", "Stopped")
                    }
                    "/pair" -> {
                        val pin = PairingManager.generatePin()
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"pin\":\"$pin\"}")
                    }
                    "/pair/verify" -> {
                        val pin = session.parameters["pin"]?.firstOrNull() ?: files["postData"]
                        if (pin != null && PairingManager.verifyPin(pin)) {
                            newFixedLengthResponse(Response.Status.OK, "text/plain", "Paired")
                        } else {
                            newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Invalid")
                        }
                    }
                    else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
                }
            }
        }
        try {
            server?.start()
            Log.i(TAG, "HTTP server started on port $port")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start HTTP server", e)
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }
}
