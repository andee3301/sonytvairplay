package com.sonyairplay.receiver

import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress
import kotlin.concurrent.thread

object MdnsAdvertiser {
    private var jmdns: JmDNS? = null

    fun start(deviceName: String = "SonyTV-AirPlay-Proto") {
        thread {
            try {
                val addr = InetAddress.getLocalHost()
                jmdns = JmDNS.create(addr, deviceName)
                val props = mutableMapOf<String, String>()
                props["deviceid"] = "00:11:22:33:44:55"
                props["features"] = "0x5A7FFFF7,0,1"
                props["model"] = "SonyBRAVIA"
                val info = ServiceInfo.create("_airplay._tcp.local.", deviceName, 7000, 0, 0, props)
                jmdns?.registerService(info)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        try {
            jmdns?.unregisterAllServices()
            jmdns?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
