package com.sonyairplay.receiver

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*

object SimpleHttpServer {
    private var server: ApplicationEngine? = null

    fun start(port: Int = 7000) {
        server = embeddedServer(CIO, port = port) {
            routing {
                get("/") {
                    call.respondText("SonyTV AirPlay Receiver", ContentType.Text.Plain)
                }
                post("/play") {
                    val body = call.receiveText()
                    println("Play request: $body")
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
        server?.start(false)
    }

    fun stop() {
        server?.stop(1000, 2000)
    }
}
