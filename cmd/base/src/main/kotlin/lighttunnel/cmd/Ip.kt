@file:JvmName("-IpKt")

package lighttunnel.cmd

import java.net.Inet4Address
import java.net.NetworkInterface

val localIpV4: String?
    get() {
        try {
            val allNetInterfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            while (allNetInterfaces.hasMoreElements()) {
                val netInterface = allNetInterfaces.nextElement() ?: continue
                if (netInterface.isLoopback || netInterface.isVirtual || !netInterface.isUp) {
                    continue
                } else {
                    val addresses = netInterface.inetAddresses ?: continue
                    while (addresses.hasMoreElements()) {
                        val ip = addresses.nextElement() ?: continue
                        if (ip is Inet4Address) {
                            return ip.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return null
    }