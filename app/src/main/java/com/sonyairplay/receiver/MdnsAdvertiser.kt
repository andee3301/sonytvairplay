package com.sonyairplay.receiver

import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress
import kotlin.concurrent.thread

object MdnsAdvertiser {
    private var jmdns: JmDNS? = null
    private var airplayInfo: ServiceInfo? = null
    private var raopInfo: ServiceInfo? = null

    fun start(deviceName: String = "SonyTV-AirPlay-Proto", airplayPort: Int = 7000, raopPort: Int = 5000) {
        thread {
            try {
                val addr = InetAddress.getLocalHost()
                jmdns = JmDNS.create(addr, deviceName)
                val props = mutableMapOf<String, String>()
                props["deviceid"] = "00:11:22:33:44:55"
                props["features"] = "0x5A7FFFF7,0,1"
                props["model"] = "SonyBRAVIA"
                airplayInfo = ServiceInfo.create("_airplay._tcp.local.", deviceName, airplayPort, 0, 0, props)
                jmdns?.registerService(airplayInfo)

                val raopProps = mutableMapOf<String, String>()
                raopProps["tp"] = "UDP"
                raopProps["txtvers"] = "1"
                raopProps["ch"] = "2"
                raopProps["am"] = "Sony BRAVIA"
                raopInfo = ServiceInfo.create("_raop._tcp.local.", deviceName, raopPort, 0, 0, raopProps)
                jmdns?.registerService(raopInfo)
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
